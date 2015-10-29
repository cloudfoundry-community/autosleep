package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@PropertySource(value = "classpath:cloudfoundry_client.properties", ignoreResourceNotFound = true)
@EnableAutoConfiguration
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

    public ClientConfiguration buildConfiguration() {
        if (isEmptyParameter(targetEndpoint) || isEmptyParameter(username) || isEmptyParameter(password)
                || isEmptyParameter(clientId)) {
            log.debug("buildConfiguration - one of the string is not provided");
            return null;
        } else {
            try {
                return new ClientConfiguration(new URL(targetEndpoint), skipSslValidation, clientId, clientSecret,
                        username, password);
            } catch (MalformedURLException m) {
                log.debug("buildConfiguration - target endpoint does not have a good syntax");
                return null;
            }
        }

    }

    private boolean isEmptyParameter(String parameter) {
        return parameter.trim().equals("");
    }
}
