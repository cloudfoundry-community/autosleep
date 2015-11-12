package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.model.ASServiceBinding;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.springframework.data.redis.core.RedisTemplate;


public class RedisBindingRepository extends RedisGenericRepository<ASServiceBinding> implements BindingRepository {


    public RedisBindingRepository(RedisTemplate<String, ASServiceBinding> redisTemplate, String storedKey) {
        super(redisTemplate, storedKey);
    }

    @Override
    protected String getObjectId(ASServiceBinding object) {
        return object.getId();
    }
}
