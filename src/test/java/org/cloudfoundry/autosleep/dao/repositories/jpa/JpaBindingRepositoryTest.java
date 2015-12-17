package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.repositories.BindingRepositoryTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"mysql","mysql-local"})
public class JpaBindingRepositoryTest extends BindingRepositoryTest {
}
