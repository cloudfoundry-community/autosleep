package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.springframework.data.repository.CrudRepository;


public interface ServiceRepository extends CrudRepository<AutosleepServiceInstance, String> {
}
