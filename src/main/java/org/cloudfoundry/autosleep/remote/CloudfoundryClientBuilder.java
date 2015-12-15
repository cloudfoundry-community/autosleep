package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CloudfoundryClientBuilder {

    @Autowired
    private Environment env;

    @Bean
    public CloudFoundryClient buildClient() {
        final String targetEndpoint = env.getProperty(Config.EnvKey.CF_ENDPOINT);
        final boolean skipSslValidation = Boolean.parseBoolean(env.getProperty(Config.EnvKey.CF_SKIP_SSL_VALIDATION,
                "false"));
        final String username = env.getProperty(Config.EnvKey.CF_USERNAME);
        final String password = env.getProperty(Config.EnvKey.CF_PASSWORD);
        final String clientId = env.getProperty(Config.EnvKey.CF_CLIENT_ID, "");
        final String clientSecret = env.getProperty(Config.EnvKey.CF_CLIENT_SECRET, "");
        try {

            log.debug("buildClient - targetEndpoint={}", targetEndpoint);
            log.debug("buildClient - skipSslValidation={}", skipSslValidation);
            log.debug("buildClient - username={}", username);
            CloudCredentials cloudCredentials;
            if (clientId.equals("") && clientSecret.equals("")) {
                log.debug("buildClient - no clientId/clientSecret provided. Using default");
                cloudCredentials = new CloudCredentials(username, password);
            } else {
                log.debug("buildClient - clientId/clientSecret provided.");
                cloudCredentials = new CloudCredentials(username, password, clientId, clientSecret);
            }
            CloudFoundryClient client = new CloudFoundryClient(cloudCredentials,
                    new URL(targetEndpoint), skipSslValidation);
            client.login();
            return client;
        } catch (MalformedURLException m) {
            log.error("CloudFoundryApi - malformed target endpoint url - {}", m.getMessage());
            return null;
        } catch (RuntimeException r) {
            log.error("CloudFoundryApi - failure while login", r);
            return null;
        }
    }
}
