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

package org.cloudfoundry.autosleep.ui.servicebroker.configuration;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AutosleepCatalogBuilder {

    @Autowired
    private Environment environment;

    @Bean
    public Catalog buildCatalog() {
        String serviceBrokerId = environment.getProperty(Config.EnvKey.CF_SERVICE_BROKER_ID,
                Config.ServiceCatalog.DEFAULT_SERVICE_BROKER_ID);

        String servicePlanId = environment.getProperty(Config.EnvKey.CF_SERVICE_PLAN_ID,
                Config.ServiceCatalog.DEFAULT_SERVICE_PLAN_ID);

        return new Catalog(Collections.singletonList(new ServiceDefinition(
                serviceBrokerId,
                serviceBrokerId,
                "Automatically stops inactive apps",
                true,
                false,
                Collections.singletonList(
                        new Plan(servicePlanId,
                                "default",
                                "Autosleep default plan",
                                null,
                                true)),
                Arrays.asList("autosleep", "document"),
                getServiceDefinitionMetadata(),
                Collections.singletonList("route_forwarding"),
                null)));
    }

    /* Used by Pivotal CF console */

    private Map<String, Object> getServiceDefinitionMetadata() {
        Map<String, Object> sdMetadata = new HashMap<>();
        sdMetadata.put("displayName", "Autosleep");
        sdMetadata.put("imageUrl", "https://en.wikipedia"
                + ".org/wiki/Sleep#/media/File:WLA_metmuseum_Bronze_statue_of_Eros_sleeping_7.jpg");
        sdMetadata.put("longDescription", "Autosleep Service");
        sdMetadata.put("providerDisplayName", "Orange");
        sdMetadata.put("documentationUrl", "https://github.com/Orange-OpenSource/autosleep");
        sdMetadata.put("supportUrl", "https://github.com/Orange-OpenSource/autosleep");
        return sdMetadata;
    }

}
