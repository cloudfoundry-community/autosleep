package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.ApplicationLocker;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.autosleep.servicebroker.service.parameters.ParameterReader;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AutosleepServiceInstanceService implements ServiceInstanceService {

    @Autowired
    private ApplicationRepository appRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private GlobalWatcher watcher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Deployment deployment;

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.IDLE_DURATION)
    private ParameterReader<Duration> idleDurationReader;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.AUTO_ENROLLMENT)
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.SECRET)
    private ParameterReader<String> secretReader;


    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request) throws
            ServiceInstanceExistsException, ServiceBrokerException {
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstance - {}", serviceId);

        if (serviceRepository.exists(serviceId)) {
            throw new ServiceInstanceExistsException(new ServiceInstance(request));
        } else {
            Config.ServiceInstanceParameters.Enrollment autoEnrollment = consumeParameter(request.getParameters(),
                    true, autoEnrollmentReader);
            String secret = consumeParameter(request.getParameters(), true, secretReader);
            Duration idleDuration = consumeParameter(request.getParameters(), true, idleDurationReader);
            Pattern excludeFromAutoEnrollment = consumeParameter(request.getParameters(), true,
                    excludeFromAutoEnrollmentReader);
            if (!request.getParameters().isEmpty()) {
                String parameterNames = String.join(", ", request.getParameters().keySet().iterator().next());
                log.debug("createServiceInstance - extra parameters are not accepted: {}", parameterNames);
                throw new InvalidParameterException(parameterNames, "Unknown parameters for creation");
            } else if (autoEnrollment == Config.ServiceInstanceParameters.Enrollment.forced) {
                checkSecuredParameter(autoEnrollmentReader.getParameterName(), secret);
            }
            SpaceEnrollerConfig serviceInstance = SpaceEnrollerConfig.builder()
                    .serviceInstanceId(request.getServiceInstanceId())
                    .serviceDefinitionId(request.getServiceDefinitionId())
                    .planId(request.getPlanId())
                    .organizationId(request.getOrganizationGuid())
                    .spaceId(request.getSpaceGuid())
                    .idleDuration(idleDuration)
                    .excludeFromAutoEnrollment(excludeFromAutoEnrollment)
                    .forcedAutoEnrollment(autoEnrollment == Config.ServiceInstanceParameters.Enrollment.forced)
                    .secret(secret != null ? passwordEncoder.encode(secret) : null)
                    .build();

            // save in repository before calling remote because otherwise local service binding controller will
            // fail retrieving the service
            serviceRepository.save(serviceInstance);
            watcher.watchServiceBindings(serviceInstance, Config.DELAY_BEFORE_FIRST_SERVICE_CHECK);
            ServiceInstance result = new ServiceInstance(request);
            if (deployment != null) {
                result.withDashboardUrl(deployment.getFirstUri() + Config.Path.DASHBOARD_CONTEXT + "/"
                        + serviceId);
            }
            return result;
        }
    }

    @Override
    public ServiceInstance getServiceInstance(String serviceInstanceId) {
        log.debug("getServiceInstance - {}", serviceInstanceId);
        SpaceEnrollerConfig serviceInstance = serviceRepository.findOne(serviceInstanceId);
        if (serviceInstance == null) {
            return null;
        } else {
            return new ServiceInstance(new CreateServiceInstanceRequest(serviceInstance.getServiceDefinitionId(),
                    serviceInstance.getPlanId(), serviceInstance.getOrganizationId(),
                    serviceInstance.getSpaceId()).withServiceInstanceId(serviceInstanceId));
        }
    }

    @Override
    public ServiceInstance updateServiceInstance(
            UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        String serviceId = request.getServiceInstanceId();
        log.debug("updateServiceInstance - {}", serviceId);
        SpaceEnrollerConfig serviceInstance = serviceRepository.findOne(serviceId);
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(serviceId);
        } else if (!serviceInstance.getPlanId().equals(request.getPlanId())) {
            /* org.cloudfoundry.community.servicebroker.model.ServiceInstance doesn't let us modify planId field
             * (private), and only handle service instance updates by re-creating them from scratch. As we need to
             * handle real updates (secret params), we are not supporting plan updates for now.*/
            throw new ServiceInstanceUpdateNotSupportedException("Service plan updates not supported.");
        } else {
            Config.ServiceInstanceParameters.Enrollment autoEnrollment = consumeParameter(request.getParameters(),
                    false, autoEnrollmentReader);
            String secret = consumeParameter(request.getParameters(), false, secretReader);
            if (!request.getParameters().isEmpty()) {
                String parameterNames = String.join(", ", request.getParameters().keySet().iterator().next());
                log.debug("updateServiceInstance - extra parameters are not accepted: {}", parameterNames);
                throw new InvalidParameterException(parameterNames, "Unknown parameters for update");
            } else if (autoEnrollment != null) {
                // only auto enrollment parameter can be updated
                checkSecuredParameter(autoEnrollmentReader.getParameterName(), secret);
                if (serviceInstance.getSecret() != null && !
                        (passwordEncoder.matches(secret, serviceInstance.getSecret())
                                ||
                                secret.equals(environment.getProperty(Config.EnvKey.SECURITY_PASSWORD)))) {
                    throw new InvalidParameterException(Config.ServiceInstanceParameters.SECRET,
                            "Provided secret does not match the one provided on creation nor the "
                                    + Config.EnvKey.SECURITY_PASSWORD + " value.");
                }
                serviceInstance.setForcedAutoEnrollment(
                        autoEnrollment == Config.ServiceInstanceParameters.Enrollment.forced);
                serviceRepository.save(serviceInstance);
            }
            return new ServiceInstance(request);
        }

    }

    @Override
    public ServiceInstance deleteServiceInstance(
            DeleteServiceInstanceRequest request) throws ServiceBrokerException {
        final String serviceInstanceId = request.getServiceInstanceId();
        log.debug("deleteServiceInstance - {}", serviceInstanceId);
        serviceRepository.delete(serviceInstanceId);

        //clean stored app linked to the service (already unbound)
        appRepository.findAll().forEach(
                aInfo -> applicationLocker.executeThreadSafe(aInfo.getUuid(), () -> {
                    ApplicationInfo applicationInfoReloaded = appRepository.findOne(aInfo.getUuid());
                    if (applicationInfoReloaded != null && !applicationInfoReloaded.getEnrollmentState()
                            .isCandidate(serviceInstanceId)) {
                        applicationInfoReloaded.getEnrollmentState().updateEnrollment(serviceInstanceId, false);
                        if (applicationInfoReloaded.getEnrollmentState().getStates().isEmpty()) {
                            appRepository.delete(applicationInfoReloaded);
                            applicationLocker.removeApplication(applicationInfoReloaded.getUuid());
                        }
                    }
                })
        );
        return new ServiceInstance(request);
    }

    private void checkSecuredParameter(String parameterName, String secret) {
        if (secret == null) {
            throw new InvalidParameterException(parameterName,
                    "Trying to set or change a protected parameter without providing the right '"
                            + Config.ServiceInstanceParameters.SECRET + "'.");
        }
    }

    private <T> T consumeParameter(Map<String, Object> parameters, boolean withDefault, ParameterReader<T> reader)
            throws InvalidParameterException {
        return reader.readParameter(parameters.remove(reader.getParameterName()), withDefault);
    }


}
