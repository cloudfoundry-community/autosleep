/**
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

import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.service.common.MysqlServiceInfo;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContextInitializerTest {
    @Mock
    private GenericApplicationContext applicationContext;

    @Mock
    private Cloud cloud;

    @Mock
    private ContextInitializer contextInitializer;


    private abstract static class MockConfigurableEnvironment implements ConfigurableEnvironment {
        @Getter
        @Setter
        private Set<String> activeProfilesContainer;

        @Override
        public void addActiveProfile(String profile) {
            activeProfilesContainer.add(profile);
        }

        @Override
        public String[] getActiveProfiles() {
            return activeProfilesContainer.toArray(new String[activeProfilesContainer.size()]);
        }
    }

    @Mock
    private MockConfigurableEnvironment configurableEnvironment;


    @Before
    public void init() throws Exception {
        when(configurableEnvironment.getActiveProfilesContainer()).thenCallRealMethod();
        when(configurableEnvironment.getActiveProfiles()).thenCallRealMethod();
        doCallRealMethod().when(configurableEnvironment).setActiveProfilesContainer(any());
        doCallRealMethod().when(configurableEnvironment).addActiveProfile(any(String.class));

        configurableEnvironment.setActiveProfilesContainer(new HashSet<>());

        when(applicationContext.getEnvironment()).thenReturn(configurableEnvironment);
        when(cloud.getServiceInfos()).thenReturn(
                Collections.singletonList(
                        new MysqlServiceInfo("mysql", "localhost")));
        doCallRealMethod().when(contextInitializer).initialize(any(GenericApplicationContext.class));

    }

    @Test
    public void testCloud() {
        when(contextInitializer.getCloud()).thenReturn(cloud);

        contextInitializer.initialize(applicationContext);

        assertThat(configurableEnvironment.getActiveProfilesContainer().size(), is(equalTo(2)));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("mysql"));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("mysql-cloud"));
    }


    @Test
    public void tesDefault() {
        when(contextInitializer.getCloud()).thenReturn(null);
        contextInitializer.initialize(applicationContext);

        assertThat(configurableEnvironment.getActiveProfilesContainer().size(), is(equalTo(1)));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("default"));
    }


}