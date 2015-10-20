package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.ServiceInstanceDaoService;
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

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
@Slf4j
public class AutosleepServiceInstanceService implements ServiceInstanceService {

    private ServiceInstanceDaoService dao;

    @Autowired
    public AutosleepServiceInstanceService(ServiceInstanceDaoService dao) {
        this.dao = dao;
    }

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request) throws
            ServiceInstanceExistsException, ServiceBrokerException {
        log.debug("createServiceInstance - {}", request.getServiceInstanceId());
        ServiceInstance serviceInstance = new ServiceInstance(request);
        if (request.getParameters() == null) {
            throw new ServiceBrokerException("No parameter given");
        }
        Duration interval = getDurationParameters(request.getParameters());
        if (interval == null) {
            //no params, or wrong params
            throw new ServiceBrokerException("'inactivity' param missing, or badly formatted (ISO-8601)");
        }
        dao.insertService(serviceInstance, interval);
        return serviceInstance;
    }

    @Override
    public ServiceInstance getServiceInstance(String serviceInstanceId) {
        log.debug("getServiceInstance - {}", serviceInstanceId);
        return dao.getService(serviceInstanceId);
    }

    @Override
    public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        log.debug("updateServiceInstance - {}", request.getServiceInstanceId());
        ServiceInstance serviceInstance = new ServiceInstance(request);
        if (request.getParameters() != null) {
            Duration interval = getDurationParameters(request.getParameters());
            if (interval == null) {
                //no params, or wrong params
                throw new ServiceBrokerException("'inactivity' param missing, or badly formatted (ISO-8601)");
            }
            dao.updateService(serviceInstance, interval);
        }
        dao.updateService(serviceInstance);
        return serviceInstance;
    }

    @Override
    public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest request) throws ServiceBrokerException {
        log.debug("deleteServiceInstance - {}", request.getServiceInstanceId());
        return dao.deleteService(request.getServiceInstanceId());
    }

    private Duration getDurationParameters(Map<String, Object> params) {
        Duration result = null;
        String inactivityPattern = (String) params.get("inactivity");
        log.debug("pattern " + inactivityPattern);
        if (inactivityPattern == null) {
            log.error("no 'inactivity' param");
        } else {
            try {
                result = Duration.parse(inactivityPattern);
            } catch (DateTimeParseException e) {
                log.error("Wrong format for inactivity duration - format should respect ISO-8601 duration format "
                        + "PnDTnHnMn");
            }
        }
        return result;
    }
}
