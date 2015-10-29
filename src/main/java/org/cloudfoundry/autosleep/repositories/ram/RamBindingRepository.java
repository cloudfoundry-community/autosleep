package org.cloudfoundry.autosleep.repositories.ram;

import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RamBindingRepository implements BindingRepository {

    private Map<String, AutoSleepServiceBinding> bindings = new HashMap<>();

    public <S extends AutoSleepServiceBinding> S save(S binding) {
        bindings.put(binding.getId(), binding);
        return binding;
    }

    @Override
    public <S extends AutoSleepServiceBinding> Iterable<S> save(Iterable<S> services) {
        List<S> result = new ArrayList<S>();
        for (S entity : services) {
            save(entity);
            result.add(entity);
        }
        return result;
    }

    @Override
    public AutoSleepServiceBinding findOne(String id) {
        return bindings.get(id);
    }

    @Override
    public boolean exists(String id) {
        return bindings.containsKey(id);
    }

    @Override
    public Iterable<AutoSleepServiceBinding> findAll() {
        return bindings.values();
    }

    @Override
    public Iterable<AutoSleepServiceBinding> findAll(Iterable<String> ids) {
        List<AutoSleepServiceBinding> bindingList = new ArrayList<>();
        for (String id : ids) {
            bindingList.add(bindings.get(id));
        }
        return bindingList;
    }

    @Override
    public long count() {
        return bindings.size();
    }

    @Override
    public void delete(String id) {
        bindings.remove(id);
    }

    @Override
    public void delete(AutoSleepServiceBinding binding) {
        bindings.remove(binding.getId());
    }

    @Override
    public void delete(Iterable<? extends AutoSleepServiceBinding> bindings) {
        for (AutoSleepServiceBinding serviceInstance : bindings) {
            delete(serviceInstance);
        }
    }

    @Override
    public void deleteAll() {
        bindings.clear();
    }

}
