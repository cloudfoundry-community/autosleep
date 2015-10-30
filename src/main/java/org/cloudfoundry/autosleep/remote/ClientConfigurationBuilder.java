package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@PropertySource(value = "classpath:cloudfoundry_client.properties", ignoreResourceNotFound = true)
@EnableAutoConfiguration
@Slf4j
public class ClientConfigurationBuilder {

    @Autowired
    private Environment environment;

    @Bean
    public ClientConfiguration buildConfiguration() throws MalformedURLException {
        final String targetEndpoint = environment.getProperty("cf.client.target.endpoint");
        final boolean skipSslValidation = Boolean.parseBoolean(environment.getProperty("cf.client.skip.ssl.validation",
                "false"));
        final String username = environment.getProperty("cf.client.username");
        final String password = environment.getProperty("cf.client.password");
        final String clientId = environment.getProperty("cf.client.clientId");
        final String clientSecret = environment.getProperty("cf.client.clientSecret");

        log.debug("buildConfiguration - targetEndpoint={}", targetEndpoint);
        log.debug("buildConfiguration - skipSslValidation={}", skipSslValidation);
        log.debug("buildConfiguration - username={}", username);

        return new ClientConfiguration(new URL(targetEndpoint), skipSslValidation, clientId, clientSecret,
                username, password);
    }
}
