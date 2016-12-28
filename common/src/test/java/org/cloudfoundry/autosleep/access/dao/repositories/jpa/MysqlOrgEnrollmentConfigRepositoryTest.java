package org.cloudfoundry.autosleep.access.dao.repositories.jpa;

import org.cloudfoundry.autosleep.access.dao.repositories.OrgEnrollmentConfigRepositoryTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;

@IfProfileValue(name = "integration-test", value = "true")
@ActiveProfiles({"mysql", "mysql-local"})
public class MysqlOrgEnrollmentConfigRepositoryTest extends OrgEnrollmentConfigRepositoryTest {

}
