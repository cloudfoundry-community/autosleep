package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@Slf4j
public class AutoSleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    private ApplicationRepository appRepository;

    private BindingRepository bindingRepository;

    private GlobalWatcher watcher;

    @Autowired
    public AutoSleepServiceInstanceBindingService(ApplicationRepository appRepository, BindingRepository
            bindingRepository, GlobalWatcher watcher) {
        this.appRepository = appRepository;
        this.bindingRepository = bindingRepository;
        this.watcher = watcher;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {

        String bindingId = request.getBindingId();
        String serviceId = request.getServiceInstanceId();
        String appId = request.getAppGuid();
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());

        ApplicationInfo appInfo = appRepository.findOne(appId);
        if (appInfo == null) {
            appInfo = new ApplicationInfo(UUID.fromString(appId));
        } else {
            appInfo.getStateMachine().onOptIn();
        }

        ApplicationBinding binding = new ApplicationBinding(bindingId,
                serviceId,
                null,
                null,
                appId);
        bindingRepository.save(binding);
        appRepository.save(appInfo);
        watcher.watchApp(binding);
        return binding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws
            ServiceBrokerException {
        String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);
        ApplicationBinding binding = bindingRepository.findOne(bindingId);
        log.debug("deleteServiceInstanceBinding on app ", binding.getAppGuid());
        ApplicationInfo appInfo = appRepository.findOne(binding.getAppGuid());
        appInfo.getStateMachine().onOptOut();
        bindingRepository.delete(bindingId);
        appRepository.save(appInfo);
        //task launched will cancel by itself
        return binding;
    }
}
