package org.cloudfoundry.autosleep.repositories.redis;

import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Profile("redis")
public class RedisServiceRepository implements ServiceRepository {
    public static final String SERVICE_KEY = "services";

    private final HashOperations<String, String, AutoSleepServiceInstance> hashOps;

    public RedisServiceRepository(RedisTemplate<String, AutoSleepServiceInstance> redisTemplate) {
        this.hashOps = redisTemplate.opsForHash();
    }

    @Override
    public <S extends AutoSleepServiceInstance> S save(S service) {
        hashOps.put(SERVICE_KEY, service.getServiceInstanceId(), service);
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
        return hashOps.get(SERVICE_KEY, id);
    }

    @Override
    public boolean exists(String id) {
        return hashOps.hasKey(SERVICE_KEY, id);
    }

    @Override
    public Iterable<AutoSleepServiceInstance> findAll() {
        return hashOps.values(SERVICE_KEY);
    }

    @Override
    public Iterable<AutoSleepServiceInstance> findAll(Iterable<String> ids) {
        return hashOps.multiGet(SERVICE_KEY, convertIterableToList(ids));
    }

    @Override
    public long count() {
        return hashOps.keys(SERVICE_KEY).size();
    }

    @Override
    public void delete(String id) {
        hashOps.delete(SERVICE_KEY, id);
    }

    @Override
    public void delete(AutoSleepServiceInstance service) {
        hashOps.delete(SERVICE_KEY, service.getServiceInstanceId());
    }

    @Override
    public void delete(Iterable<? extends AutoSleepServiceInstance> services) {
        for (AutoSleepServiceInstance serviceInstance : services) {
            delete(serviceInstance);
        }
    }

    @Override
    public void deleteAll() {
        Set<String> ids = hashOps.keys(SERVICE_KEY);
        for (String id : ids) {
            delete(id);
        }
    }

    private <T> List<T> convertIterableToList(Iterable<T> iterable) {
        List<T> list = new ArrayList<T>();
        for (T object : iterable) {
            list.add(object);
        }
        return list;
    }

}
