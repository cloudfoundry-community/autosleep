package org.cloudfoundry.autosleep.repositories.redis;

import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class RedisBindingRepository implements BindingRepository {
    public static final String BINDING_KEY = "binding_store";

    private final HashOperations<String, String, AutoSleepServiceBinding> hashOps;

    public RedisBindingRepository(RedisTemplate<String, AutoSleepServiceBinding> redisTemplate) {
        this.hashOps = redisTemplate.opsForHash();
    }

    @Override
    public <S extends AutoSleepServiceBinding> S save(S binding) {
        hashOps.put(BINDING_KEY, binding.getId(), binding);
        return binding;
    }

    @Override
    public <S extends AutoSleepServiceBinding> Iterable<S> save(Iterable<S> binding) {
        List<S> result = new ArrayList<S>();

        for (S entity : binding) {
            save(entity);
            result.add(entity);
        }

        return result;
    }

    @Override
    public AutoSleepServiceBinding findOne(String id) {
        return hashOps.get(BINDING_KEY, id);
    }

    @Override
    public boolean exists(String id) {
        return hashOps.hasKey(BINDING_KEY, id);
    }

    @Override
    public Iterable<AutoSleepServiceBinding> findAll() {
        return hashOps.values(BINDING_KEY);
    }

    @Override
    public Iterable<AutoSleepServiceBinding> findAll(Iterable<String> ids) {
        return hashOps.multiGet(BINDING_KEY, convertIterableToList(ids));
    }

    @Override
    public long count() {
        return hashOps.keys(BINDING_KEY).size();
    }

    @Override
    public void delete(String id) {
        hashOps.delete(BINDING_KEY, id);
    }

    @Override
    public void delete(AutoSleepServiceBinding service) {
        hashOps.delete(BINDING_KEY, service.getId());
    }

    @Override
    public void delete(Iterable<? extends AutoSleepServiceBinding> services) {
        for (AutoSleepServiceBinding serviceInstance : services) {
            delete(serviceInstance);
        }
    }

    @Override
    public void deleteAll() {
        Set<String> ids = hashOps.keys(BINDING_KEY);
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
