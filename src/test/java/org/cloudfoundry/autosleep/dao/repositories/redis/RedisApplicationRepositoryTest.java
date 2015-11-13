package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.repositories.AppRepositoryTest;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"redis","redis-local"})
public class RedisApplicationRepositoryTest extends AppRepositoryTest {

    @BeforeClass
    public static void skipIfNoRedis() {
        Assume.assumeTrue("Redis should be present to run this test", RedisUtil.isRedisPresent());
    }
}