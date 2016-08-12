/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.access.cloudfoundry.config;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.DefaultClientIdentification;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.logging.LoggingClient;
import org.cloudfoundry.spring.client.SpringCloudFoundryClient;
import org.cloudfoundry.spring.logging.SpringLoggingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource(value = "classpath:cloudfoundry_client.properties", ignoreResourceNotFound = true)
@Slf4j
public class CloudfoundryClientBuilder {

    @Builder
    @Getter
    private static class ClientContainer {

        private CloudFoundryClient client;

        private LoggingClient logClient;

        @Builder
        ClientContainer(CloudFoundryClient client,
                        LoggingClient logClient) {
            this.client = client;
            this.logClient = logClient;
        }
    }

    private ClientContainer clientContainer;

    @Autowired
    private Environment env;

    private RuntimeException initializationError;

    private synchronized ClientContainer buildIfNeeded() {
        if (clientContainer == null && initializationError == null) {
            final String targetHost = env.getProperty(Config.EnvKey.CF_HOST);
            final boolean skipSslValidation = Boolean.parseBoolean(env.getProperty(
                    Config.EnvKey.CF_SKIP_SSL_VALIDATION,
                    Boolean.FALSE.toString()));
            final String username = env.getProperty(Config.EnvKey.CF_USERNAME);
            final String password = env.getProperty(Config.EnvKey.CF_PASSWORD);
            final String clientId = env.getProperty(Config.EnvKey.CF_CLIENT_ID, DefaultClientIdentification.ID);
            final String clientSecret = env.getProperty(Config.EnvKey.CF_CLIENT_SECRET, DefaultClientIdentification
                    .SECRET);
            try {

                log.debug("buildClient - targetHost={}", targetHost);
                log.debug("buildClient - skipSslValidation={}", skipSslValidation);
                log.debug("buildClient - username={}", username);
                SpringCloudFoundryClient client = SpringCloudFoundryClient.builder()
                        .host(targetHost)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .skipSslValidation(skipSslValidation)
                        .username(username)
                        .password(password)
                        .build();

                SpringLoggingClient logClient = SpringLoggingClient.builder().cloudFoundryClient(client).build();
                this.clientContainer = ClientContainer.builder()
                        .client(client)
                        .logClient(logClient)
                        .build();
                return this.clientContainer;
            } catch (RuntimeException r) {
                log.error("CloudFoundryApi - failure while login", r);
                initializationError = new ApplicationContextException("Failed to build client", r);
                throw initializationError;
            }
        } else if (initializationError != null) {
            throw initializationError;
        } else {
            return clientContainer;
        }
    }

    @Bean
    @ConditionalOnMissingBean(CloudFoundryClient.class)
    public CloudFoundryClient getClient() {
        return buildIfNeeded().getClient();
    }

    @Bean
    @ConditionalOnMissingBean(LoggingClient.class)
    public LoggingClient getLogClient() {
        return buildIfNeeded().getLogClient();
    }

}
