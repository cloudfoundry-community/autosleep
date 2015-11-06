package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AutoSleepServiceInstanceService implements ServiceInstanceService {

    private ServiceRepository repository;

    @Autowired
    public AutoSleepServiceInstanceService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request) throws
            ServiceInstanceExistsException, ServiceBrokerException {
        log.debug("createServiceInstance - {}", request.getServiceInstanceId());

        AutoSleepServiceInstance serviceInstance = repository.findOne(request.getServiceInstanceId());
        if (serviceInstance != null) {
            throw new ServiceInstanceExistsException(serviceInstance);
        } else {
            serviceInstance = new AutoSleepServiceInstance(request);
            repository.save(serviceInstance);
        }
        return serviceInstance;
    }

    @Override
    public ServiceInstance getServiceInstance(String serviceInstanceId) {
        log.debug("getServiceInstance - {}", serviceInstanceId);
        return repository.findOne(serviceInstanceId);
    }

    @Override
    public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        String serviceId = request.getServiceInstanceId();
        log.debug("updateServiceInstance - {}", serviceId);
        AutoSleepServiceInstance serviceInstance = repository.findOne(serviceId);
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(serviceId);
        } else {
            serviceInstance = new AutoSleepServiceInstance(request);
            repository.save(serviceInstance);
        }
        return serviceInstance;
    }

    @Override
    public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest request) throws ServiceBrokerException {
        log.debug("deleteServiceInstance - {}", request.getServiceInstanceId());
        AutoSleepServiceInstance serviceInstance = new AutoSleepServiceInstance(request);
        repository.delete(request.getServiceInstanceId());
        return serviceInstance;
    }


}
