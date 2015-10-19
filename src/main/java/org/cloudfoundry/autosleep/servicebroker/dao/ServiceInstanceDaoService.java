package org.cloudfoundry.autosleep.servicebroker.dao;

import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

/**
 * Created by buce8373 on 15/10/2015.
 */
public interface ServiceInstanceDaoService {

    interface ReadCallback<T> {
        void read(T element);
    }

    void insertService(ServiceInstance serviceInstance) throws ServiceInstanceExistsException;

    ServiceInstance getService(String serviceInstanceId);

    void listServices(ReadCallback<ServiceInstance> callback);

    void updateService(ServiceInstance serviceInstance) throws ServiceInstanceUpdateNotSupportedException,
            ServiceInstanceDoesNotExistException;

    ServiceInstance deleteService(String serviceInstanceId);

    void addBinding(String serviceInstanceId, ServiceInstanceBinding serviceInstanceBinding) throws
            ServiceInstanceBindingExistsException;

    void listBinding(String serviceInstanceId, ReadCallback<ServiceInstanceBinding> callback) throws
            ServiceInstanceDoesNotExistException;

    ServiceInstanceBinding removeBinding(String serviceInstanceId, String serviceBindingId);

    void purge();

}
