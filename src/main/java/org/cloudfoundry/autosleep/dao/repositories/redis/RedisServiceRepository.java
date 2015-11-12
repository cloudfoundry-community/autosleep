package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.model.ASServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.springframework.data.redis.core.RedisTemplate;


public class RedisServiceRepository extends RedisGenericRepository<ASServiceInstance> implements ServiceRepository {


    public RedisServiceRepository(RedisTemplate<String, ASServiceInstance> redisTemplate, String storedKey) {
        super(redisTemplate, storedKey);
    }

    @Override
    protected String getObjectId(ASServiceInstance object) {
        return object.getServiceInstanceId();
    }
}
