package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.Clock;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class AutosleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    @Autowired
    protected Clock clock;

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws
            ServiceInstanceBindingExistsException, ServiceBrokerException {
        String bindingId = request.getBindingId();
        String serviceId = request.getServiceInstanceId();
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());
        ServiceInstanceBinding serviceInstanceBinding = new ServiceInstanceBinding(bindingId,
                serviceId,
                null/*TODO credentials*/,
                ""/*TODO log url*/,
                request.getAppGuid());
        Runnable logRunnable = () -> log.info("--this is the click from a service biding instance {} <--> {}",
                serviceId,
                request.getBindingId());
        clock.startTimer(request.getBindingId(), 0, 1, TimeUnit.SECONDS, logRunnable);
        return serviceInstanceBinding;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws
            ServiceBrokerException {
        log.debug("deleteServiceInstanceBinding - {}", request.getBindingId());
        clock.stopTimer(request.getBindingId());
        return null;
    }
}
