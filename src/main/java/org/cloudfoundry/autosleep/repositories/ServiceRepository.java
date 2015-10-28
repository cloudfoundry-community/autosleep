package org.cloudfoundry.autosleep.repositories;

import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.springframework.data.repository.CrudRepository;


public interface ServiceRepository extends CrudRepository<AutoSleepServiceInstance, String> {
}
