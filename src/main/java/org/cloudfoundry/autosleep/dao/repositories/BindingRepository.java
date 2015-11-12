package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.ASServiceBinding;
import org.springframework.data.repository.CrudRepository;


public interface BindingRepository extends CrudRepository<ASServiceBinding, String> {
}
