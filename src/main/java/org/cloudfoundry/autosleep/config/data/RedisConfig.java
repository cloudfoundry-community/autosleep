package org.cloudfoundry.autosleep.config.data;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ASServiceBinding;
import org.cloudfoundry.autosleep.dao.model.ASServiceInstance;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.dao.repositories.redis.RedisApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.redis.RedisBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.redis.RedisServiceRepository;
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
    public ServiceRepository redisServiceRepository(RedisTemplate<String, ASServiceInstance> redisTemplate) {
        return new RedisServiceRepository(redisTemplate, "service_store");
    }

    @Bean
    public BindingRepository redisBindingRepository(RedisTemplate<String, ASServiceBinding> redisTemplate) {
        return new RedisBindingRepository(redisTemplate, "binding_store");
    }

    @Bean
    public RedisApplicationRepository redisAppRepository(RedisTemplate<String, ApplicationInfo> redisTemplate) {
        return new RedisApplicationRepository(redisTemplate, "app_store");
    }

    private final RedisSerializer<String> stringSerializer = new StringRedisSerializer();

    /**
     * Init serializers for ServiceInstances in Redis.
     */
    @Bean
    public RedisTemplate<String, ASServiceInstance> serviceRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ASServiceInstance> template = getStringKeyTemplate(factory);
        RedisSerializer<ASServiceInstance> serviceSerializer = new Jackson2JsonRedisSerializer<>(
                ASServiceInstance.class);
        template.setValueSerializer(serviceSerializer);
        template.setHashValueSerializer(serviceSerializer);
        return template;
    }

    /**
     * Init serializers for ServiceBindings in Redis.
     */
    @Bean
    public RedisTemplate<String, ASServiceBinding> bindingRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ASServiceBinding> template = getStringKeyTemplate(factory);
        RedisSerializer<ASServiceBinding> bindingSerializer = new Jackson2JsonRedisSerializer<>(ASServiceBinding.class);
        template.setValueSerializer(bindingSerializer);
        template.setHashValueSerializer(bindingSerializer);
        return template;
    }

    /**
     * Init serializers for ApplicationInfos in Redis.
     */
    @Bean
    public RedisTemplate<String, ApplicationInfo> appInfoRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ApplicationInfo> template = getStringKeyTemplate(factory);
        RedisSerializer<ApplicationInfo> appSerializer = new Jackson2JsonRedisSerializer<>(ApplicationInfo.class);
        template.setValueSerializer(appSerializer);
        template.setHashValueSerializer(appSerializer);
        return template;
    }

    protected <T> RedisTemplate<String, T> getStringKeyTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        return template;
    }
}
