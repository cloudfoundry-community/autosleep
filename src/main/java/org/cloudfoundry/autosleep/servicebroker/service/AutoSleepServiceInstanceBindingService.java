package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.AppStateChecker;
import org.cloudfoundry.autosleep.scheduling.Clock;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
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

    private Clock clock;
    private CloudFoundryApiService remote;
    private ServiceRepository serviceRepository;
    private BindingRepository bindingRepository;


    /** Constructor with autowired args.*/
    @Autowired
    public AutoSleepServiceInstanceBindingService(Clock clock, CloudFoundryApiService remote, ServiceRepository
            serviceRepository, BindingRepository bindingRepository) {
        this.clock = clock;
        this.remote = remote;
        this.serviceRepository = serviceRepository;
        this.bindingRepository = bindingRepository;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {

        String bindingId = request.getBindingId();
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());
        AutoSleepServiceBinding binding = new AutoSleepServiceBinding(bindingId,
                serviceId,
                null/*TODO credentials?*/,
                null,
                request.getAppGuid());

        bindingRepository.save(binding);

        AppStateChecker checker = new AppStateChecker(UUID.fromString(request.getAppGuid()),
                request.getBindingId(),
                serviceRepository.findOne(serviceId).getInterval(),
                remote,
                clock);
        checker.start();

        return binding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws
            ServiceBrokerException {
        String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);
        AutoSleepServiceBinding binding = bindingRepository.findOne(bindingId);
        bindingRepository.delete(bindingId);
        clock.stopTimer(bindingId);
        return binding;
    }
}
