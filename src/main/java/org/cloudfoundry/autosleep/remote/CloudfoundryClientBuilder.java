package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
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
    private Environment environment;

    @Bean
    public CloudFoundryClient buildClient() {
        final String targetEndpoint = environment.getProperty("cf.client.target.endpoint");
        final boolean skipSslValidation = Boolean.parseBoolean(environment.getProperty("cf.client.skip.ssl.validation",
                "false"));
        final String username = environment.getProperty("cf.client.username");
        final String password = environment.getProperty("cf.client.password");
        final String clientId = environment.getProperty("cf.client.clientId", "");
        final String clientSecret = environment.getProperty("cf.client.clientSecret", "");
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
