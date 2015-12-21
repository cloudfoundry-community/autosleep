package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaBindingRepository extends JpaRepository<ApplicationBinding, String>, BindingRepository {
}
