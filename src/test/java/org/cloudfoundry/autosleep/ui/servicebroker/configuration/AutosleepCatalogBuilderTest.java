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
import org.cloudfoundry.autosleep.config.Config.EnvKey;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = AutosleepCatalogBuilder.class)
public class AutosleepCatalogBuilderTest {

    private static final String SERVICE_BROKER_ID = UUID.randomUUID().toString();

    private static final String SERVICE_PLAN_ID = UUID.randomUUID().toString();

    @InjectMocks
    private AutosleepCatalogBuilder catalogBuilder;

    @Mock
    private Environment environment;

    @Test
    public void test_catalog_is_built() {
        //Given environment variable are configured
        when(environment.getProperty(eq(Config.EnvKey.CF_SERVICE_BROKER_ID), anyString()))
                .thenReturn(SERVICE_BROKER_ID);
        when(environment.getProperty(eq(EnvKey.CF_SERVICE_PLAN_ID), anyString()))
                .thenReturn(SERVICE_PLAN_ID);
        //When catalog is built
        Catalog catalog = catalogBuilder.buildCatalog();

        //Then catalog contains the good values
        assertThat(catalog.getServiceDefinitions().size(), is(equalTo(1)));
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertThat(serviceDefinition.getId(), is(equalTo(SERVICE_BROKER_ID)));
        assertThat(serviceDefinition.getPlans().size(), is(equalTo(1)));
        assertTrue(serviceDefinition.isBindable());
        assertFalse(serviceDefinition.isPlanUpdateable());
        Plan plan = serviceDefinition.getPlans().get(0);
        assertThat(plan.getId(), is(equalTo(SERVICE_PLAN_ID)));
        assertTrue(plan.isFree());
        assertThat(serviceDefinition.getDashboardClient(), is(nullValue()));
    }

}
