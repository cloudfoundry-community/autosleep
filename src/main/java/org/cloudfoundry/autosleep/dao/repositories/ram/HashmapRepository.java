package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class HashmapRepository<T> implements CrudRepository<T, String> {

    abstract String getObjectId(T object);

    private Map<String, T> store = new HashMap<>();

    public <S extends T> S save(S object) {
        store.put(getObjectId(object), object);
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
        return store.get(id);
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public Iterable<T> findAll() {
        return store.values();
    }

    @Override
    public Iterable<T> findAll(Iterable<String> ids) {
        List<T> objects = new ArrayList<>();
        for (String id : ids) {
            objects.add(store.get(id));
        }
        return objects;
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public void delete(T object) {
        store.remove(getObjectId(object));
    }

    @Override
    public void delete(Iterable<? extends T> objects) {
        for (T entity : objects) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

}
