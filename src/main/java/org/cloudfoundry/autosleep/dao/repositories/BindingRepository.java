package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.Binding;
import org.springframework.data.repository.CrudRepository;


public interface BindingRepository extends CrudRepository<Binding, String> {
}
