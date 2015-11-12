package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public abstract class RedisGenericRepository<T> implements CrudRepository<T, String> {

    private final HashOperations<String, String, T> hashOps;
    private final String storedKey;

    public RedisGenericRepository(RedisTemplate<String, T> redisTemplate, String storedKey) {
        this.hashOps = redisTemplate.opsForHash();
        this.storedKey = storedKey;
    }

    abstract String getObjectId(T object);

    @Override
    public <S extends T> S save(S object) {
        hashOps.put(storedKey, getObjectId(object), object);
        return object;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> objects) {
        List<S> result = new ArrayList<S>();
        for (S entity : objects) {
            save(entity);
            result.add(entity);
        }
        return result;
    }

    @Override
    public T findOne(String id) {
        return hashOps.get(storedKey, id);
    }

    @Override
    public boolean exists(String id) {
        return hashOps.hasKey(storedKey, id);
    }

    @Override
    public Iterable<T> findAll() {
        return hashOps.values(storedKey);
    }

    @Override
    public Iterable<T> findAll(Iterable<String> ids) {
        return hashOps.multiGet(storedKey, convertIterableToList(ids));
    }

    @Override
    public long count() {
        return hashOps.keys(storedKey).size();
    }

    @Override
    public void delete(String id) {
        hashOps.delete(storedKey, id);
    }

    @Override
    public void delete(T object) {
        hashOps.delete(storedKey, getObjectId(object));
    }

    @Override
    public void delete(Iterable<? extends T> objects) {
        for (T serviceInstance : objects) {
            delete(serviceInstance);
        }
    }

    @Override
    public void deleteAll() {
        Set<String> ids = hashOps.keys(storedKey);
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
