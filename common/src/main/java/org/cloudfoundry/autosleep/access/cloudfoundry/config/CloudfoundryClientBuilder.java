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
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource(value = "classpath:cloudfoundry_client.properties", ignoreResourceNotFound = true)
@EnableAutoConfiguration
@Slf4j
public class CloudfoundryClientBuilder {

    @Builder
    @Getter
    private static class ClientContainer {

        private CloudFoundryClient cloudFoundryClient;

        private DopplerClient dopplerClient;

        @Builder
        ClientContainer(CloudFoundryClient cloudFoundryClient,
                        DopplerClient dopplerClient) {
            this.cloudFoundryClient = cloudFoundryClient;
            this.dopplerClient = dopplerClient;
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
                ConnectionContext connectionContext = DefaultConnectionContext.builder()
                        .apiHost(targetHost)
                        .skipSslValidation(skipSslValidation)
                        .build();
                TokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                        .username(username)
                        .password(password)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();

                CloudFoundryClient client = ReactorCloudFoundryClient.builder()
                        .connectionContext(connectionContext)
                        .tokenProvider(tokenProvider)
                        .build();
                DopplerClient dopplerClient = ReactorDopplerClient.builder()
                        .connectionContext(connectionContext)
                        .tokenProvider(tokenProvider)
                        .build();

                this.clientContainer = ClientContainer.builder()
                        .cloudFoundryClient(client)
                        .dopplerClient(dopplerClient)
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
    public CloudFoundryClient cloudFoundryClient() {
        return buildIfNeeded().getCloudFoundryClient();
    }

    @Bean
    @ConditionalOnMissingBean(DopplerClient.class)
    public DopplerClient dopplerClient() {
        return buildIfNeeded().getDopplerClient();
    }

}
