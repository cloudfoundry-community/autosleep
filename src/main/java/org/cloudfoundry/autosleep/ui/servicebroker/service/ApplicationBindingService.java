package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceBindingResource;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class ApplicationBindingService implements ServiceInstanceBindingService {

    @Autowired
    private ApplicationRepository appRepository;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private WorkerManagerService workerManager;

    @Autowired
    private ApplicationLocker applicationLocker;


    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {

        final String bindingId = request.getBindingId();
        final String configId = request.getServiceInstanceId();

        log.debug("createServiceInstanceBinding - {}", bindingId);
        SpaceEnrollerConfig spaceEnrollerConfig = spaceEnrollerConfigRepository.findOne(configId);

        String targetAppId = (String) request.getBindResource().get(ServiceBindingResource.BIND_RESOURCE_KEY_APP.toString());
        String routeId  = (String) request.getBindResource().get(ServiceBindingResource.BIND_RESOURCE_KEY_ROUTE.toString());
        if (targetAppId != null) {
            log.debug("creating binding {} for app {}", bindingId, targetAppId);
            ApplicationBinding binding = ApplicationBinding.builder().serviceInstanceId(configId)
                    .serviceBindingId(bindingId)
                    .applicationId(targetAppId).build();
            applicationLocker.executeThreadSafe(targetAppId, () -> {
                ApplicationInfo appInfo = appRepository.findOne(targetAppId);
                if (appInfo == null) {
                    appInfo = new ApplicationInfo(targetAppId);
                }

                appInfo.getEnrollmentState().addEnrollmentState(configId);

                //retrieve service to return its params as credentials

                bindingRepository.save(binding);
                appRepository.save(appInfo);
                workerManager.registerApplicationStopper(spaceEnrollerConfig, targetAppId);
            });
            return new CreateServiceInstanceBindingResponse(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION,
                    spaceEnrollerConfig.getIdleDuration().toString()));
        } else if (routeId != null) {
            log.debug("creating binding {} for route {}", bindingId, routeId);
            String proxyRoute="";
            //TODO ROUTESERVICE get extra parameter appId and appBindingId(else exception)
            /*TODO check app know :
            ApplicationInfo applicationInfo = appRepo.findapp()
            if(applicationInfo == null) {
                throw new ServiceBrokerException("Only Autosleep is allowed to bind route to itself");
            }*/
            //TODO ROUTE SERVICE create proxy route?
            /*TODO store binding RouteBinding binding = new RouteBinding(bindingId,
                    configId,
                    routeId,
                    appId,
                    appBindingId, proxyRoute);*/


            return new CreateServiceInstanceBindingResponse(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION,
                    spaceEnrollerConfig.getIdleDuration().toString()), proxyRoute);
        } else {
            throw new ServiceBrokerException("Unknown bind ressource");
        }

    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
            throws ServiceBrokerException {
        final String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);

        //TODO ROUTE SERVICE if unroute app, check if need to unroute routes and call ourself

        final ApplicationBinding binding = bindingRepository.findOne(bindingId);
        final String appId = binding.getApplicationId();

        SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository.findOne(request.getServiceInstanceId());

        applicationLocker.executeThreadSafe(appId, () -> {
            log.debug("deleteServiceInstanceBinding on app ", appId);
            ApplicationInfo appInfo = appRepository.findOne(appId);
            if (appInfo != null) {
                appInfo.getEnrollmentState().updateEnrollment(serviceInstance.getId(),
                        !serviceInstance.isForcedAutoEnrollment());
                if (appInfo.getEnrollmentState().getStates().size() == 0) {
                    appRepository.delete(appId);
                    applicationLocker.removeApplication(appId);
                } else {
                    appRepository.save(appInfo);
                }
            } else {
                log.error("Deleting a binding with no related application info. This should never happen.");
            }
            bindingRepository.delete(bindingId);

            //task launched will cancel by itself
        });
    }
}
