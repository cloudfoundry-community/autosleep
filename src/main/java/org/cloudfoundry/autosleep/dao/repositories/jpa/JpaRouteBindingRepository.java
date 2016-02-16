package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.model.RouteBinding;
import org.cloudfoundry.autosleep.dao.repositories.RouteBindingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRouteBindingRepository extends JpaRepository<RouteBinding, String>, RouteBindingRepository {
}
