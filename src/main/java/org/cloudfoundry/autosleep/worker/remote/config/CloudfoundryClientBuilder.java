package org.cloudfoundry.autosleep.worker.remote.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.client.spring.SpringCloudFoundryClient;
import org.cloudfoundry.client.spring.SpringLoggingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
@PropertySource(value = "classpath:cloudfoundry_client.properties", ignoreResourceNotFound = true)
@EnableAutoConfiguration
@Slf4j
public class CloudfoundryClientBuilder {

    @Getter(onMethod = @__(@Bean))
    private SpringCloudFoundryClient cfClient;

    @Autowired
    private Environment env;

    @Getter(onMethod = @__(@Bean))
    private SpringLoggingClient logClient;

    @PostConstruct
    public void initClients() {
        final String targetEndpoint = env.getProperty(Config.EnvKey.CF_ENDPOINT);
        final boolean skipSslValidation = Boolean.parseBoolean(env.getProperty(Config.EnvKey.CF_SKIP_SSL_VALIDATION,
                "false"));
        final String username = env.getProperty(Config.EnvKey.CF_USERNAME);
        final String password = env.getProperty(Config.EnvKey.CF_PASSWORD);
        final String clientId = env.getProperty(Config.EnvKey.CF_CLIENT_ID, "cf");
        final String clientSecret = env.getProperty(Config.EnvKey.CF_CLIENT_SECRET, "");
        try {

            log.debug("buildClient - targetEndpoint={}", targetEndpoint);
            log.debug("buildClient - skipSslValidation={}", skipSslValidation);
            log.debug("buildClient - username={}", username);
            cfClient = SpringCloudFoundryClient.builder()
                    .host(targetEndpoint)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .skipSslValidation(skipSslValidation)
                    .username(username)
                    .password(password).build();

            logClient = SpringLoggingClient.builder().cloudFoundryClient(cfClient).build();
        } catch (RuntimeException r) {
            log.error("CloudFoundryApi - failure while login", r);
        }
    }

}
