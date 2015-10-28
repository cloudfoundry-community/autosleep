package org.cloudfoundry.autosleep.repositories;

import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.springframework.data.repository.CrudRepository;


public interface BindingRepository extends CrudRepository<AutoSleepServiceBinding, String> {
}
