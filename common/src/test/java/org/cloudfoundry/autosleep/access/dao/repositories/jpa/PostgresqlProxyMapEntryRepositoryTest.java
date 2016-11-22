package org.cloudfoundry.autosleep.access.dao.repositories.jpa;

import org.cloudfoundry.autosleep.access.dao.repositories.ProxyMapEntryRepositoryTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;

@IfProfileValue(name = "integration-test", value = "true")
@ActiveProfiles({"postgresql", "postgresql-local"})
public class PostgresqlProxyMapEntryRepositoryTest extends ProxyMapEntryRepositoryTest {

}