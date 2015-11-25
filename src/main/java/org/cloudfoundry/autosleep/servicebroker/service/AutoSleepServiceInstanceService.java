package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@Slf4j
public class AutoSleepServiceInstanceService implements ServiceInstanceService {

    private ApplicationRepository appRepository;

    private ServiceRepository serviceRepository;

    private GlobalWatcher watcher;

    private PasswordEncoder passwordEncoder;

    private Deployment deployment;


    @Autowired
    public AutoSleepServiceInstanceService(ApplicationRepository appRepository,
                                           ServiceRepository serviceRepository,
                                           GlobalWatcher watcher,
                                           PasswordEncoder passwordEncoder,
                                           Deployment deployment) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.watcher = watcher;
        this.passwordEncoder = passwordEncoder;
        this.deployment = deployment;
    }

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request) throws
            ServiceInstanceExistsException, ServiceBrokerException {
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstance - {}", serviceId);

        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceId);
        if (serviceInstance != null) {
            throw new ServiceInstanceExistsException(serviceInstance);
        } else {
            request.setParameters(processParameters(null, request.getParameters()));
            serviceInstance = new AutosleepServiceInstance(request);
            String baseDashUrl = deployment == null ? "localhost:8080" : deployment.getFirstUri();
            serviceInstance.withDashboardUrl(baseDashUrl + Config.Path.dashboardPrefix + serviceId);

            // save in repository before calling remote because otherwise local service binding controller will
            // fail retrieving the service
            serviceRepository.save(serviceInstance);
            watcher.watchServiceBindings(serviceId, Config.delayBeforeFirstServiceCheck);
        }
        return serviceInstance;
    }

    @Override
    public ServiceInstance getServiceInstance(String serviceInstanceId) {
        log.debug("getServiceInstance - {}", serviceInstanceId);
        return serviceRepository.findOne(serviceInstanceId);
    }

    @Override
    public ServiceInstance updateServiceInstance(
            UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        String serviceId = request.getServiceInstanceId();
        log.debug("updateParams - {}", serviceId);
        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceId);
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(serviceId);
        } else {
            request.setParameters(processParameters(serviceInstance.getSecretHash(), request.getParameters()));
            if (!serviceInstance.getPlanId().equals(request.getPlanId())) {
                /* org.cloudfoundry.community.servicebroker.model.ServiceInstance doesn't let us modify planId field
                 * (private), and only handle service instance updates by re-creating them from scratch. As we need to
                 * handle real updates (secret params), we are not supporting plan updates for now.*/
                throw new ServiceInstanceUpdateNotSupportedException("Service plan updates not supported.");
            }
            serviceInstance.updateFromParameters(request.getParameters());
            serviceRepository.save(serviceInstance);
        }
        return serviceInstance;
    }

    @Override
    public ServiceInstance deleteServiceInstance(
            DeleteServiceInstanceRequest request) throws ServiceBrokerException {
        log.debug("deleteServiceInstance - {}", request.getServiceInstanceId());
        AutosleepServiceInstance serviceInstance = new AutosleepServiceInstance(request);
        serviceRepository.delete(request.getServiceInstanceId());

        //clean stored app linked to the service (already unbound)
        appRepository.findAll().forEach(
                aInfo -> {
                    if (aInfo.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
                        appRepository.delete(aInfo);
                    }
                }
        );
        return serviceInstance;
    }

    private Map<String, Object> processParameters(String existingSecret, Map<String, Object> parameters) {
        Map<String, Object> result = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
        processSecret(existingSecret, result);
        processInactivity(result);
        processExcludeNames(result);
        processNoOptOut(result);
        return result;
    }

    private void processSecret(String existingSecret, Map<String, Object> params) {
        if (existingSecret != null
                &&
                (
                        params.get(AutosleepServiceInstance.SECRET_PARAMETER) == null
                                ||
                                !passwordEncoder.matches((String) params.get(AutosleepServiceInstance.SECRET_PARAMETER),
                                        existingSecret)
                )) {
            throw new InvalidParameterException(AutosleepServiceInstance.SECRET_PARAMETER,
                    "Provided parameter  does not match the provided one on creation.");
        } else if (params.get(AutosleepServiceInstance.SECRET_PARAMETER) != null) {
            params.put(AutosleepServiceInstance.SECRET_PARAMETER,
                    passwordEncoder.encode((String) params.get(AutosleepServiceInstance.SECRET_PARAMETER)));
        }
    }


    private void processInactivity(Map<String, Object> params) throws InvalidParameterException {
        if (params.get(AutosleepServiceInstance.INACTIVITY_PARAMETER) != null) {
            String inactivityPattern = (String) params.get(AutosleepServiceInstance.INACTIVITY_PARAMETER);
            log.debug("pattern " + inactivityPattern);
            try {
                params.put(AutosleepServiceInstance.INACTIVITY_PARAMETER, Duration.parse(inactivityPattern));
            } catch (DateTimeParseException e) {
                log.error("Wrong format for inactivity duration - format should respect ISO-8601 duration format "
                        + "PnDTnHnMn");
                throw new InvalidParameterException(AutosleepServiceInstance.INACTIVITY_PARAMETER,
                        "param badly formatted (ISO-8601). Example: \"PT15M\" for 15mn");
            }
        }
    }

    private void processExcludeNames(Map<String, Object> params) throws InvalidParameterException {
        Pattern excludeNames = null;
        if (params.get(AutosleepServiceInstance.EXCLUDE_PARAMETER) != null) {
            String excludeNamesStr = (String) params.get(AutosleepServiceInstance.EXCLUDE_PARAMETER);
            if (!excludeNamesStr.trim().equals("")) {
                log.debug("excludeNames " + excludeNamesStr);
                try {
                    excludeNames = Pattern.compile(excludeNamesStr);
                } catch (PatternSyntaxException p) {
                    log.error("Wrong format for exclusion  - format cannot be compiled to a valid regexp");
                    throw new InvalidParameterException(AutosleepServiceInstance.EXCLUDE_PARAMETER,
                            "should be a valid regexp");
                }
            }
        }
        params.put(AutosleepServiceInstance.EXCLUDE_PARAMETER, excludeNames);
    }

    private void processNoOptOut(Map<String, Object> params) throws InvalidParameterException {
        if (params.get(AutosleepServiceInstance.NO_OPTOUT_PARAMETER) != null) {
            checkSecuredParameter(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, params);
            String noOptOut = (String) params.get(AutosleepServiceInstance.NO_OPTOUT_PARAMETER);
            log.debug("noOptOut " + noOptOut);
            params.put(AutosleepServiceInstance.NO_OPTOUT_PARAMETER,
                    Boolean.parseBoolean((String) params.get(AutosleepServiceInstance.NO_OPTOUT_PARAMETER)));
        }
    }

    private void checkSecuredParameter(String parameterName, Map<String, Object> params) {
        if (params.get(AutosleepServiceInstance.SECRET_PARAMETER) == null) {
            throw new InvalidParameterException(parameterName,
                    "Trying to set or change a protected parameter without providing the right '"
                            + AutosleepServiceInstance.SECRET_PARAMETER + "'.");
        }
    }


}
