package org.cloudfoundry.autosleep.config.data;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.repositories.redis.RedisBindingRepository;
import org.cloudfoundry.autosleep.repositories.redis.RedisServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@Profile("redis")
public class RedisConfig {

    @PostConstruct
    public void logProfile() {
        log.info("<<<<<<<<<<<<<<  loading REDIS persistence profile  >>>>>>>>>>>>>");
    }

    @Bean
    public ServiceRepository redisServiceRepository(RedisTemplate<String, AutoSleepServiceInstance> redisTemplate) {
        return new RedisServiceRepository(redisTemplate);
    }

    @Bean
    public BindingRepository redisBindingRepository(RedisTemplate<String, AutoSleepServiceBinding> redisTemplate) {
        return new RedisBindingRepository(redisTemplate);
    }


    @Bean
    public RedisTemplate<String, AutoSleepServiceInstance> serviceRedisTemplate(RedisConnectionFactory
                                                                                        redisConnectionFactory) {

        RedisTemplate<String, AutoSleepServiceInstance> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        RedisSerializer<AutoSleepServiceInstance> serviceSerializer = new Jackson2JsonRedisSerializer<>(
                AutoSleepServiceInstance.class);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(serviceSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(serviceSerializer);
        return template;
    }

    @Bean
    public RedisTemplate<String, AutoSleepServiceBinding> bindingRedisTemplate(RedisConnectionFactory
                                                                                       redisConnectionFactory) {
        RedisTemplate<String, AutoSleepServiceBinding> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        RedisSerializer<AutoSleepServiceBinding> serviceSerializer = new Jackson2JsonRedisSerializer<>(
                AutoSleepServiceBinding.class);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(serviceSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(serviceSerializer);

        return template;
    }

}
