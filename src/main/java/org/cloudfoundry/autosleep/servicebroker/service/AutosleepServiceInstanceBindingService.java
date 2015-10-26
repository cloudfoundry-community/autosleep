package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.ServiceInstanceDaoService;
import org.cloudfoundry.autosleep.remote.CloudFoundryApi;
import org.cloudfoundry.autosleep.scheduling.AppStateChecker;
import org.cloudfoundry.autosleep.scheduling.Clock;
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
public class AutosleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    protected Clock clock;

    private ServiceInstanceDaoService dao;

    private CloudFoundryApi remote;

    /** Constructor with autowired args.*/
    @Autowired
    public AutosleepServiceInstanceBindingService(ServiceInstanceDaoService dao, Clock clock, CloudFoundryApi remote) {
        this.dao = dao;
        this.clock = clock;
        this.remote = remote;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {

        String bindingId = request.getBindingId();
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());
        ServiceInstanceBinding serviceInstanceBinding = new ServiceInstanceBinding(bindingId,
                serviceId,
                null/*TODO credentials*/,
                null,
                request.getAppGuid());

        dao.addBinding(serviceId, serviceInstanceBinding);

        AppStateChecker checker = new AppStateChecker(request.getAppGuid(),
                request.getBindingId(),
                dao.getServiceInstanceInactivityParam(serviceId),
                remote,
                clock);
        checker.start();

        return serviceInstanceBinding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws
            ServiceBrokerException {
        log.debug("deleteServiceInstanceBinding - {}", request.getBindingId());

        ServiceInstanceBinding result = dao.removeBinding(request.getInstance().getServiceInstanceId(),
                request.getBindingId());
        clock.stopTimer(request.getBindingId());
        return result;
    }
}
