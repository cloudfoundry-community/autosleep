package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.repositories.AppRepositoryTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"redis","redis-local"})
public class RedisApplicationRepositoryTest extends AppRepositoryTest {

}