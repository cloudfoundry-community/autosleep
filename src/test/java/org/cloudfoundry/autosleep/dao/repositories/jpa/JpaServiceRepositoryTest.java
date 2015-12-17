package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.repositories.ServiceRepositoryTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"mysql","mysql-local"})
public class JpaServiceRepositoryTest extends ServiceRepositoryTest {
}
