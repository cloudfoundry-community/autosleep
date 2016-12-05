package org.cloudfoundry.autosleep.access.dao.repositories.jpa;

import org.cloudfoundry.autosleep.access.dao.repositories.EnrolledOrganizationConfigRepositoryTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;

@IfProfileValue(name = "integration-test", value = "true")
@ActiveProfiles({"postgresql", "postgresql-local"})
public class PostgresqlEnrolledOrganizationConfigRepositoryTest extends EnrolledOrganizationConfigRepositoryTest {

}
