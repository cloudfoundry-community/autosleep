package org.cloudfoundry.autosleep.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.client.model.ClientConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:cloundfoundry_client.properties", ignoreResourceNotFound = true)
@EnableAutoConfiguration
@Getter
@Slf4j
public class ClientConfigurationBuilder {
    @Value("${target.endpoint:}")
    private String targetEndpoint;

    @Value("${skip.ssl.validation:false}")
    private boolean skipSslValidation;

    @Value("${username:}")
    private String username;

    @Value("${password:}")
    private String password;

    @Value("${client.id:}")
    private String clientId;

    @Value("${client.secret:}")
    private String clientSecret;

    @Bean
    public ClientConfiguration buildConfiguration() {
        return new ClientConfiguration(targetEndpoint, skipSslValidation, clientId, clientSecret);
    }

}
