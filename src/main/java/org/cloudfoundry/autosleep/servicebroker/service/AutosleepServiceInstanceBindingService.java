package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

/**
 * Created by BUCE8373 on 13/10/2015.
 */
@Service
@Slf4j
public class AutosleepServiceInstanceBindingService implements ServiceInstanceBindingService {

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) throws ServiceInstanceBindingExistsException, ServiceBrokerException {
        log.debug("createServiceInstanceBinding - {}", request.getBindingId());
        return null;
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) throws ServiceBrokerException {
        log.debug("deleteServiceInstanceBinding - {}", request.getBindingId());
        return null;
    }
}
