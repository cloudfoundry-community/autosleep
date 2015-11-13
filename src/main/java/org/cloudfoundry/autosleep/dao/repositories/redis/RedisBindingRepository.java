package org.cloudfoundry.autosleep.dao.repositories.redis;

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.springframework.data.redis.core.RedisTemplate;


public class RedisBindingRepository extends RedisGenericRepository<ApplicationBinding> implements BindingRepository {


    public RedisBindingRepository(RedisTemplate<String, ApplicationBinding> redisTemplate, String storedKey) {
        super(redisTemplate, storedKey);
    }

    @Override
    protected String getObjectId(ApplicationBinding object) {
        return object.getId();
    }
}
