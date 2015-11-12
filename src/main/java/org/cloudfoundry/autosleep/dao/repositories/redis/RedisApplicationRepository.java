package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.springframework.data.redis.core.RedisTemplate;


public class RedisApplicationRepository extends RedisGenericRepository<ApplicationInfo> implements
        ApplicationRepository {

    public RedisApplicationRepository(RedisTemplate<String, ApplicationInfo> redisTemplate, String storedKey) {
        super(redisTemplate, storedKey);
    }

    @Override
    protected String getObjectId(ApplicationInfo object) {
        return object.getUuid().toString();
    }
}
