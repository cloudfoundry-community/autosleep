package org.cloudfoundry.autosleep.dao.repositories.jpa;

import org.cloudfoundry.autosleep.dao.repositories.BindingRepositoryTest;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"mysql", "mysql-local"})
public class JpaBindingRepositoryTest extends BindingRepositoryTest {
    @BeforeClass
    public static void skipIfNoMysql() {
        Assume.assumeTrue("Mysql should be present to run this test", JpaUtil.isMySqlPresent());
    }
}
