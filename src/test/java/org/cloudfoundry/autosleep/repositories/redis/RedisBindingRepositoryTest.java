package org.cloudfoundry.autosleep.repositories.redis;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.repositories.ServiceRepositoryTest;
import org.junit.BeforeClass;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles({"redis","redis-local"})
public class RedisBindingRepositoryTest extends ServiceRepositoryTest {

    @BeforeClass
    public static void skipIfNoRedis() {
        org.junit.Assume.assumeTrue("Redis should be present to run this test", RedisUtil.isRedisPresent());
    }
}
