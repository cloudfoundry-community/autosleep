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
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
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
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {
        final String appId = request.getAppGuid();
        final String bindingId = request.getBindingId();
        final String configId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", bindingId);
        SpaceEnrollerConfig spaceEnrollerConfig = spaceEnrollerConfigRepository.findOne(configId);

        ApplicationBinding binding = ApplicationBinding.builder().serviceInstanceId(configId)
                .serviceBindingId(bindingId)
                .applicationId(appId).build();
        applicationLocker.executeThreadSafe(appId, () -> {
            ApplicationInfo appInfo = appRepository.findOne(appId);
            if (appInfo == null) {
                appInfo = new ApplicationInfo(appId);
            }

            appInfo.getEnrollmentState().addEnrollmentState(configId);

            //retrieve service to return its params as credentials

            bindingRepository.save(binding);
            appRepository.save(appInfo);
            workerManager.registerApplicationStopper(spaceEnrollerConfig, appId);
        });

        return new ServiceInstanceBinding(bindingId, configId,
                Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION,
                        spaceEnrollerConfig.getIdleDuration().toString()), null, appId);
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
            throws ServiceBrokerException {
        final String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);

        final ApplicationBinding binding = bindingRepository.findOne(bindingId);
        final String appId = binding.getApplicationId();

        SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository
                .findOne(request.getInstance().getServiceInstanceId());

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
        return new ServiceInstanceBinding(binding.getServiceBindingId(), binding.getServiceInstanceId(),
                Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION,
                        serviceInstance.getIdleDuration().toString()), null, appId);
    }
}
