package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
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


@Service
@Slf4j
public class AutoSleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    private BindingRepository bindingRepository;

    private GlobalWatcher watcher;

    @Autowired
    public AutoSleepServiceInstanceBindingService(BindingRepository bindingRepository, GlobalWatcher watcher) {
        this.bindingRepository = bindingRepository;
        this.watcher = watcher;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {

        String bindingId = request.getBindingId();
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());
        ApplicationBinding binding = new ApplicationBinding(bindingId,
                serviceId,
                null,
                null,
                request.getAppGuid());
        bindingRepository.save(binding);
        watcher.watchApp(binding);
        return binding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws
            ServiceBrokerException {
        String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);
        ApplicationBinding binding = bindingRepository.findOne(bindingId);
        bindingRepository.delete(bindingId);
        //task launched will unbind by itself
        return binding;
    }
}
