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

import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeployedApplicationConfigTest {

    private static final String APPLICATION_NAME = "test";

    private static final UUID APP_ID = UUID.randomUUID();

    private static final String[] URIS = {"somewhere.org", "somewhere-else.org", "nowhere.org"};

    @InjectMocks
    private DeployedApplicationConfig config;

    @Mock
    private Environment environment;

    @Test
    public void test_read_current_deployment_with_but_no_uris() throws Exception {
        //Given property is present without uris
        when(environment.getProperty(eq(Config.EnvKey.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY)))
                .thenReturn(BeanGenerator.getSampleVcapApplication(APP_ID, APPLICATION_NAME));
        //When deployment is done
        DeployedApplicationConfig.Deployment deployment = config.loadCurrentDeployment();
        //Then deployment is not null but  does not contain any uri
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getApplicationUris(), is(notNullValue()));
        assertThat(deployment.getApplicationUris().size(), is(equalTo(0)));
        assertThat(deployment.getFirstUri(), is(nullValue()));
    }

    @Test
    public void test_read_current_deployment_with_uris() throws Exception {
        //Given property is present without uris
        when(environment.getProperty(eq(Config.EnvKey.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY)))
                .thenReturn(BeanGenerator.getSampleVcapApplication(APP_ID, APPLICATION_NAME, URIS));
        //When deployment is done
        DeployedApplicationConfig.Deployment deployment = config.loadCurrentDeployment();

        //Then deployment is not null and contains any uri
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getApplicationId(), is(equalTo(APP_ID.toString())));
        assertThat(deployment.getApplicationName(), is(equalTo(APPLICATION_NAME)));
        assertThat(deployment.getApplicationUris(), is(notNullValue()));
        assertThat(deployment.getApplicationUris().size(), is(equalTo(URIS.length)));
        assertThat(deployment.getFirstUri(), is(equalTo(URIS[0])));

    }

    @Test
    public void test_read_current_deployment_without_property() throws Exception {
        //Given property is not present
        when(environment.getProperty(eq(Config.EnvKey.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY)))
                .thenReturn(null);
        //When deployment is done
        DeployedApplicationConfig.Deployment deployment = config.loadCurrentDeployment();
        //Then deployment returns nothing
        assertThat(deployment, is(nullValue()));
    }

}