package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.springframework.data.repository.CrudRepository;


public interface SpaceEnrollerConfigRepository extends CrudRepository<SpaceEnrollerConfig, String> {
}
