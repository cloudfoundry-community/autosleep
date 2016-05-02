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

package org.cloudfoundry.autosleep.access.dao.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.access.dao.config.RepositoryConfig;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.util.ApplicationConfiguration;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class ApplicationRepositoryTest extends CrudRepositoryTest<ApplicationInfo> {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    protected ApplicationInfo build(String id) {
        ApplicationInfo result = BeanGenerator.createAppInfoLinkedToService(id, "APTestServiceId");
        result.updateDiagnosticInfo(
                BeanGenerator.createAppLog(),
                BeanGenerator.createCloudEvent(),
                "appName",
                CloudFoundryAppState.STARTED);
        result.getEnrollmentState().addEnrollmentState("serviceId");
        return result;
    }

    @Override
    protected void compareReloaded(ApplicationInfo original, ApplicationInfo reloaded) {
        assertThat(reloaded.getUuid(), is(equalTo(original.getUuid())));
        if (original.getEnrollmentState() != null) {
            assertThat(reloaded.getEnrollmentState(), is(notNullValue()));
            assertThat(reloaded.getEnrollmentState().getStates(), is(notNullValue()));
            assertThat(reloaded.getEnrollmentState().getStates().size(), is(equalTo(original.getEnrollmentState()
                    .getStates().size())));
        }
        assertThat(reloaded, is(equalTo(original)));
    }

    /**
     * test hibernate issue: storing an "empty" embedded object with every field null will result in a null object
     * when retrieved from db.
     */
    @Test
    public void empty_diagnostic_info_should_not_be_null_when_read_from_db() {
        //given that an app has an "empty" diagnostic is saved to database
        String uuid = UUID.randomUUID().toString();
        ApplicationInfo info = ApplicationInfo.builder().uuid(uuid).build();
        assertThat(info.getDiagnosticInfo(), is(notNullValue()));
        applicationRepository.save(info);

        //when we retrieve it from db
        ApplicationInfo retrievedInfo = applicationRepository.findOne(uuid);

        //then the empty diagnosticInfo is not null
        assertThat(retrievedInfo.getDiagnosticInfo(), is(notNullValue()));
    }

    @Before
    @After
    public void setAndClearDao() {
        setDao(applicationRepository);
        applicationRepository.deleteAll();
    }

    @Test
    public void test_count_by_app_ids() {
        //Given db contains some entity
        List<String> ids = Arrays.asList("testCountId1", "testCountId2", "testCountId3");
        ids.forEach(id -> applicationRepository.save(build(id)));
        //When we count entity providing some ids
        long count = applicationRepository.countByApplicationIds(ids.subList(0, 2));
        //Then we get the expected result
        assertThat((int) count, is(equalTo(2)));

    }

}