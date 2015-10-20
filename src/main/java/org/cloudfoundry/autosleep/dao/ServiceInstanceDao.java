package org.cloudfoundry.autosleep.dao;

import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by buce8373 on 15/10/2015.
 */
@Service
public class ServiceInstanceDao implements ServiceInstanceDaoService {


    private Map<String, ServiceInstanceContainer> serviceInstances = new HashMap<>();
    

    @Override
    public void insertService(ServiceInstance serviceInstance) throws ServiceInstanceExistsException {
        if (serviceInstances.containsKey(serviceInstance.getServiceInstanceId())) {
            throw new ServiceInstanceExistsException(serviceInstance);
        } else {
            serviceInstances.put(serviceInstance.getServiceInstanceId(), new ServiceInstanceContainer(serviceInstance));
        }

    }

    @Override
    public ServiceInstance getService(String serviceInstanceId) {
        ServiceInstanceContainer container = serviceInstances.get(serviceInstanceId);
        if (container == null) {
            return null;
        } else {
            return container.getServiceInstance();
        }
    }

    @Override
    public void listServices(ReadCallback<ServiceInstance> callback) {
        serviceInstances.values().forEach(container -> callback.read(container.getServiceInstance()));
    }

    @Override
    public void updateService(ServiceInstance serviceInstance) throws ServiceInstanceDoesNotExistException,
            ServiceInstanceUpdateNotSupportedException {
        //Due to a bug in service broker layer, we won't support
        //TODO: support it when pull request accepted: https://github
        // .com/cloudfoundry-community/spring-boot-cf-service-broker/pull/35
        throw new ServiceInstanceUpdateNotSupportedException("update not supported");
    }

    @Override
    public ServiceInstance deleteService(String serviceInstanceId) {
        ServiceInstanceContainer container = serviceInstances.remove(serviceInstanceId);
        if (container == null) {
            return null;
        } else {
            return container.getServiceInstance();
        }
    }

    @Override
    public void addBinding(String serviceInstanceId, ServiceInstanceBinding serviceInstanceBinding) throws
            ServiceInstanceBindingExistsException {
        ServiceInstanceContainer container = serviceInstances.get(serviceInstanceId);
        if (container != null) {
            if (container.getBindings().containsKey(serviceInstanceBinding.getId())) {
                throw new ServiceInstanceBindingExistsException(serviceInstanceBinding);
            } else {
                container.getBindings().put(serviceInstanceBinding.getId(), serviceInstanceBinding);
            }
        }
    }

    @Override
    public void listBinding(String serviceInstanceId, ReadCallback<ServiceInstanceBinding> callback) throws
            ServiceInstanceDoesNotExistException {
        getContainer(serviceInstanceId).getBindings().values().forEach(binding -> callback.read(binding));
    }

    @Override
    public ServiceInstanceBinding removeBinding(String serviceInstanceId, String serviceBindingId) {
        ServiceInstanceContainer container = serviceInstances.get(serviceInstanceId);
        if (container == null) {
            return null;
        } else {
            return container.getBindings().remove(serviceBindingId);
        }
    }

    @Override
    public void purge() {
        serviceInstances.clear();
    }

    private ServiceInstanceContainer getContainer(String serviceInstanceId) throws
            ServiceInstanceDoesNotExistException {
        ServiceInstanceContainer container = serviceInstances.get(serviceInstanceId);
        if (container == null) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        } else {
            return container;
        }
    }

}
