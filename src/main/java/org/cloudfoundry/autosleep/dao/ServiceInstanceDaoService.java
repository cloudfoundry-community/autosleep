package org.cloudfoundry.autosleep.dao;

import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.time.Duration;


public interface ServiceInstanceDaoService {

    interface ReadCallback<T> {
        void read(T element);
    }

    void insertService(ServiceInstance serviceInstance, Duration interval) throws
            ServiceInstanceExistsException;

    ServiceInstance getService(String serviceInstanceId);

    Duration getServiceInstanceInactivityParam(String serviceInstanceId);

    void listServices(ReadCallback<ServiceInstance> callback);

    void updateService(ServiceInstance serviceInstance, Duration interval) throws
            ServiceInstanceUpdateNotSupportedException,
            ServiceInstanceDoesNotExistException;

    void updateService(ServiceInstance serviceInstance) throws
            ServiceInstanceUpdateNotSupportedException,
            ServiceInstanceDoesNotExistException;

    ServiceInstance deleteService(String serviceInstanceId);

    void addBinding(String serviceInstanceId, ServiceInstanceBinding serviceInstanceBinding) throws
            ServiceInstanceBindingExistsException;

    void listBinding(String serviceInstanceId, ReadCallback<ServiceInstanceBinding> callback) throws
            ServiceInstanceDoesNotExistException;

    ServiceInstanceBinding removeBinding(String serviceInstanceId, String serviceBindingId);

    void purge();

}
