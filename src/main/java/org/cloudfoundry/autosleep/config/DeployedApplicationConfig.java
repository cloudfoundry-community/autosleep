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

package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.List;

@Configuration
@Slf4j
public class DeployedApplicationConfig {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class Deployment {

        @JsonProperty("application_id")
        private String applicationId;

        @JsonProperty("application_name")
        private String applicationName;

        @JsonProperty("application_uris")
        private List<String> applicationUris;

        public String getFirstUri() {
            if (applicationUris == null || applicationUris.size() == 0) {
                return null;
            } else {
                return applicationUris.get(0);
            }
        }

    }

    @Autowired
    private Environment environment;

    @Bean
    public Deployment loadCurrentDeployment() throws IOException {
        String deployment = environment.getProperty(Config.EnvKey.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY);
        if (deployment == null) {
            return null;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(deployment, Deployment.class);
        }
    }
}
