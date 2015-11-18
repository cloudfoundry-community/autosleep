package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
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

    private ApplicationRepository appRepository;

    private ServiceRepository serviceRepository;

    private GlobalWatcher watcher;


    @Autowired
    public AutoSleepServiceInstanceService(ApplicationRepository appRepository, ServiceRepository serviceRepository,
                                           GlobalWatcher watcher) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.watcher = watcher;
    }

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request) throws
            ServiceInstanceExistsException, ServiceBrokerException {
        log.debug("createServiceInstance - {}", request.getServiceInstanceId());

        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(request.getServiceInstanceId());
        if (serviceRepository.findOne(request.getServiceInstanceId()) != null) {
            throw new ServiceInstanceExistsException(serviceInstance);
        } else {
            serviceInstance = new AutosleepServiceInstance(request);
            // save in repository before calling remote because otherwise local service binding controller will
            // fail retrieving the service
            serviceRepository.save(serviceInstance);
            watcher.watchServiceBindings(request.getServiceInstanceId(), Config.delayBeforeFirstServiceCheck);
        }
        return serviceInstance;
    }

    @Override
    public ServiceInstance getServiceInstance(String serviceInstanceId) {
        log.debug("getServiceInstance - {}", serviceInstanceId);
        return serviceRepository.findOne(serviceInstanceId);
    }

    @Override
    public ServiceInstance updateServiceInstance(
            UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        String serviceId = request.getServiceInstanceId();
        log.debug("updateServiceInstance - {}", serviceId);
        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceId);
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(serviceId);
        } else {
            serviceInstance = new AutosleepServiceInstance(request);
            serviceRepository.save(serviceInstance);
        }
        return serviceInstance;
    }

    @Override
    public ServiceInstance deleteServiceInstance(
            DeleteServiceInstanceRequest request) throws ServiceBrokerException {
        log.debug("deleteServiceInstance - {}", request.getServiceInstanceId());
        AutosleepServiceInstance serviceInstance = new AutosleepServiceInstance(request);
        serviceRepository.delete(request.getServiceInstanceId());

        //clean stored app linked to the service (already unbound)
        appRepository.findAll().forEach(
                aInfo -> {
                    if (aInfo.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
                        appRepository.delete(aInfo);
                    }
                }
        );
        return serviceInstance;
    }


}
