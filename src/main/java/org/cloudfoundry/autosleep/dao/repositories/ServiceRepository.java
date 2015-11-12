package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.ASServiceInstance;
import org.springframework.data.repository.CrudRepository;


public interface ServiceRepository extends CrudRepository<ASServiceInstance, String> {
}
