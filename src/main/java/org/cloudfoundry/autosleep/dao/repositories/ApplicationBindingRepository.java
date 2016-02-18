package org.cloudfoundry.autosleep.dao.repositories;

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.springframework.data.repository.CrudRepository;

public interface ApplicationBindingRepository extends CrudRepository<ApplicationBinding, String> {

}
