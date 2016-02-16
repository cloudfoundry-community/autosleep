package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.RouteBinding;
import org.springframework.data.repository.CrudRepository;

public interface RouteBindingRepository extends CrudRepository<RouteBinding, String> {
}
