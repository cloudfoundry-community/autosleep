package org.cloudfoundry.autosleep.config;

import lombok.Getter;
import lombok.Setter;
import org.cloudfoundry.autosleep.dao.repositories.redis.RedisUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.service.common.RedisServiceInfo;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;


import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class ContextInitializerTest {
    @Mock
    private GenericApplicationContext applicationContext;

    @Mock
    private Cloud cloud;

    @Mock
    private ContextInitializer contextInitializer;


    private static abstract class MockConfigurableEnvironment implements ConfigurableEnvironment {
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
                        new RedisServiceInfo("redis", "localhost", RedisUtil.REDIS_DEFAULT_PORT, "password")));
        doCallRealMethod().when(contextInitializer).initialize(any(GenericApplicationContext.class));

    }

    @Test
    public void testRedisCloud(){
        when(contextInitializer.getCloud()).thenReturn(cloud);

        contextInitializer.initialize(applicationContext);

        assertThat(configurableEnvironment.getActiveProfilesContainer().size(), is(equalTo(2)));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("redis"));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("redis-cloud"));
    }

    @Test
    public void testRedisLocal(){
        when(contextInitializer.getCloud()).thenReturn(null);
        configurableEnvironment.getActiveProfilesContainer().add("redis");
        contextInitializer.initialize(applicationContext);

        assertThat(configurableEnvironment.getActiveProfilesContainer().size(), is(equalTo(2)));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("redis"));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("redis-local"));
    }

    @Test
    public void tesDefault(){
        when(contextInitializer.getCloud()).thenReturn(null);
        contextInitializer.initialize(applicationContext);

        assertThat(configurableEnvironment.getActiveProfilesContainer().size(), is(equalTo(1)));
        assertTrue(configurableEnvironment.getActiveProfilesContainer().contains("default"));
    }



}