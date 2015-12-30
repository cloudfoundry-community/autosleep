package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.repositories.BindingRepositoryTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;

@IfProfileValue(name = "integration-test", value = "true")
@ActiveProfiles({"mysql", "mysql-local"})
public class MysqlBindingRepositoryTest extends BindingRepositoryTest {

}
