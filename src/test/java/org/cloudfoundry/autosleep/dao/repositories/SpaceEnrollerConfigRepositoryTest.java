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

package org.cloudfoundry.autosleep.dao.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.util.ApplicationConfiguration;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class SpaceEnrollerConfigRepositoryTest extends CrudRepositoryTest<SpaceEnrollerConfig> {

    private static final String ORG_TEST = "orgTest";

    private static final String SERVICE_DEFINITION_ID = "TESTS";

    private static final String SERVICE_PLAN_ID = "PLAN";

    private static final String SPACE_TEST = "spaceTest";

    private Duration duration = Duration.ofMinutes(15);

    private Pattern excludePattern = Pattern.compile(".*");

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Override
    protected SpaceEnrollerConfig build(String id) {
        return SpaceEnrollerConfig.builder()
                .excludeFromAutoEnrollment(excludePattern)
                .id(id)
                .idleDuration(duration)
                .organizationId(ORG_TEST)
                .planId(SERVICE_PLAN_ID)
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .spaceId(SPACE_TEST)
                .build();
    }

    @Override
    protected void compareReloaded(SpaceEnrollerConfig original, SpaceEnrollerConfig reloaded) {
        assertEquals(reloaded.getId(), original.getId());
        //Beware: Pattern.equals directly calls Object.equals
        if (original.getExcludeFromAutoEnrollment() == null) {
            assertThat(reloaded.getExcludeFromAutoEnrollment(), is(nullValue()));
        } else {
            assertThat(reloaded.getExcludeFromAutoEnrollment(), is(notNullValue()));
            assertEquals(reloaded.getExcludeFromAutoEnrollment().pattern(), original.getExcludeFromAutoEnrollment()
                    .pattern());
        }
        assertEquals(reloaded.getIdleDuration(), original.getIdleDuration());
        assertEquals(reloaded.getOrganizationId(), original.getOrganizationId());
        assertEquals(reloaded.getPlanId(), original.getPlanId());
        assertEquals(reloaded.getServiceDefinitionId(), original.getServiceDefinitionId());
        assertEquals(reloaded.getSpaceId(), original.getSpaceId());
        assertThat("Two objects should be equal", reloaded, is(equalTo(original)));
    }

    @Before
    @After
    public void setAndClearDao() {
        setDao(spaceEnrollerConfigRepository);
        spaceEnrollerConfigRepository.deleteAll();
    }

}