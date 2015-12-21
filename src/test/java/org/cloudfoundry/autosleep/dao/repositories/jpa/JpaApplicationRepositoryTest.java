package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.repositories.AppRepositoryTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"mysql","mysql-local"})
public class JpaApplicationRepositoryTest extends AppRepositoryTest{

}