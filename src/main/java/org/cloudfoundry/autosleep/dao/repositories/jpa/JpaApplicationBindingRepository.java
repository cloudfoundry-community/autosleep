package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationBindingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaApplicationBindingRepository extends JpaRepository<ApplicationBinding, String>,
        ApplicationBindingRepository {
}
