package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
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
public class AutosleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    protected Clock clock;

    private CloudFoundryApiService remote;

    private ServiceRepository serviceRepository;


    /** Constructor with autowired args.*/
    @Autowired
    public AutosleepServiceInstanceBindingService(Clock clock, CloudFoundryApiService remote,ServiceRepository serviceRepository) {
        this.clock = clock;
        this.remote = remote;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {

        String bindingId = request.getBindingId();
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());
        AutoSleepServiceBinding serviceInstanceBinding = new AutoSleepServiceBinding(bindingId,
                serviceId,
                null/*TODO credentials*/,
                null,
                request.getAppGuid());

        //TODO dao.addBinding(serviceId, serviceInstanceBinding);

        AppStateChecker checker = new AppStateChecker(UUID.fromString(request.getAppGuid()),
                request.getBindingId(),
                serviceRepository.findOne(serviceId).getInterval(),
                remote,
                clock);
        checker.start();

        return serviceInstanceBinding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws
            ServiceBrokerException {
        log.debug("deleteServiceInstanceBinding - {}", request.getBindingId());
        /*TODO remove binding ServiceInstanceBinding result = dao.removeBinding(request.getInstance().getServiceInstanceId(),
                request.getBindingId());*/
        clock.stopTimer(request.getBindingId());
        //TODO return unbinded object
        return null;
    }
}
