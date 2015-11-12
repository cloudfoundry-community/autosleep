package org.cloudfoundry.autosleep.dao.repositories.redis;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepositoryTest;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles({"redis","redis-local"})
public class RedisBindingRepositoryTest extends BindingRepositoryTest {

    @BeforeClass
    public static void skipIfNoRedis() {
        Assume.assumeTrue("Redis should be present to run this test", RedisUtil.isRedisPresent());
    }
}
