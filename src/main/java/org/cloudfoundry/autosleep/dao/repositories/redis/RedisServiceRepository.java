package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.springframework.data.redis.core.RedisTemplate;


public class RedisServiceRepository extends RedisGenericRepository<AutosleepServiceInstance>
        implements ServiceRepository {


    public RedisServiceRepository(RedisTemplate<String, AutosleepServiceInstance> redisTemplate, String storedKey) {
        super(redisTemplate, storedKey);
    }

    @Override
    protected String getObjectId(AutosleepServiceInstance object) {
        return object.getServiceInstanceId();
    }
}
