package org.cloudfoundry.autosleep.repositories.ram;

import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Profile("in-memory")
public class RamServiceRepository implements ServiceRepository {

    private Map<String, AutoSleepServiceInstance> serviceInstances = new HashMap<>();

    public <S extends AutoSleepServiceInstance> S save(S service) {
        serviceInstances.put(service.getServiceInstanceId(), service);
        return service;
    }

    @Override
    public <S extends AutoSleepServiceInstance> Iterable<S> save(Iterable<S> services) {
        List<S> result = new ArrayList<S>();

        for (S entity : services) {
            save(entity);
            result.add(entity);
        }

        return result;
    }

    @Override
    public AutoSleepServiceInstance findOne(String id) {
        return serviceInstances.get(id);
    }

    @Override
    public boolean exists(String id) {
        return serviceInstances.containsKey(id);
    }

    @Override
    public Iterable<AutoSleepServiceInstance> findAll() {
        return serviceInstances.values();
    }

    @Override
    public Iterable<AutoSleepServiceInstance> findAll(Iterable<String> ids) {
        List<AutoSleepServiceInstance> services = new ArrayList<>();
        for (String id : ids) {
            services.add(serviceInstances.get(id));
        }
        return services;
    }

    @Override
    public long count() {
        return serviceInstances.size();
    }

    @Override
    public void delete(String id) {
        serviceInstances.remove(id);
    }

    @Override
    public void delete(AutoSleepServiceInstance service) {
        serviceInstances.remove(service.getServiceInstanceId());
    }

    @Override
    public void delete(Iterable<? extends AutoSleepServiceInstance> services) {
        for (AutoSleepServiceInstance serviceInstance : services) {
            delete(serviceInstance);
        }
    }

    @Override
    public void deleteAll() {
        serviceInstances.clear();
    }

}
