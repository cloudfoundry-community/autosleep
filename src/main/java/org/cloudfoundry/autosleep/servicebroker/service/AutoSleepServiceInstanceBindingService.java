package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.ApplicationLocker;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
@Slf4j
public class AutosleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    private ApplicationRepository appRepository;

    private ServiceRepository serviceRepository;

    private BindingRepository bindingRepository;

    private GlobalWatcher watcher;

    private ApplicationLocker applicationLocker;

    @Autowired
    public AutosleepServiceInstanceBindingService(ApplicationRepository appRepository,
                                                  ServiceRepository serviceRepository,
                                                  BindingRepository bindingRepository,
                                                  GlobalWatcher watcher,
                                                  ApplicationLocker applicationLocker) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.bindingRepository = bindingRepository;
        this.watcher = watcher;
        this.applicationLocker = applicationLocker;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {
        final String appId = request.getAppGuid();
        final String bindingId = request.getBindingId();
        final String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", bindingId);
        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceId);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Config.ServiceInstanceParameters.IDLE_DURATION, serviceInstance.getInterval().toString());

        final ApplicationBinding binding = new ApplicationBinding(bindingId,
                serviceId,
                credentials,
                null,
                appId);
        applicationLocker.executeThreadSafe(appId, () -> {
            ApplicationInfo appInfo = appRepository.findOne(appId);
            if (appInfo == null) {
                appInfo = new ApplicationInfo(UUID.fromString(appId));
            }

            appInfo.addBoundService(serviceId);

            //retrieve service to return its params as credentials

            bindingRepository.save(binding);
            appRepository.save(appInfo);
            watcher.watchApp(binding);
        });

        return binding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
            throws ServiceBrokerException {
        final String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);

        final ApplicationBinding binding = bindingRepository.findOne(bindingId);
        final String appId = binding.getAppGuid();

        AutosleepServiceInstance serviceInstance = serviceRepository
                .findOne(request.getInstance().getServiceInstanceId());

        applicationLocker.executeThreadSafe(appId, () -> {
            log.debug("deleteServiceInstanceBinding on app ", appId);
            ApplicationInfo appInfo = appRepository.findOne(appId);
            if (appInfo != null) {
                appInfo.removeBoundService(serviceInstance.getServiceInstanceId(), !serviceInstance.isNoOptOut());
                if (appInfo.getServiceInstances().size() == 0) {
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
        return binding;
    }
}
