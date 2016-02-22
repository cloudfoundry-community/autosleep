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

package org.cloudfoundry.autosleep.dao.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.util.ApplicationConfiguration;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class ServiceRepositoryTest {

    private static final String ORG_TEST = "orgTest";
    private static final String SPACE_TEST = "spaceTest";
    private static final String SERVICE_DEFINITION_ID = "TESTS";
    private static final String SERVICE_PLAN_ID = "PLAN";

    @Autowired
    private SpaceEnrollerConfigRepository dao;

    private enum InsertedInstanceIds {
        testInsertServiceFail, testGetServiceSuccess, testUpdateServiceSuccess,
        testDeleteServiceByIdSuccess,
        testDeleteServiceByInstanceSuccess,
        testDeleteMass01,
        testDeleteMass02,
    }

    private Duration duration = Duration.ofMinutes(15);
    private Pattern excludePattern = Pattern.compile(".*");


    private long nbServicesInit;


    /**
     * Init DAO with test data.
     */
    @Before
    public void populateDao() {
        Duration duration = Duration.ofMinutes(15);
        Pattern excludePattern = Pattern.compile(".*");

        dao.deleteAll();

        Arrays.asList(InsertedInstanceIds.values()).forEach(serviceInstanceId -> dao.save(
                SpaceEnrollerConfig.builder()
                        .serviceDefinitionId(SERVICE_DEFINITION_ID)
                        .planId(SERVICE_PLAN_ID)
                        .organizationId(ORG_TEST)
                        .spaceId(SPACE_TEST)
                        .id(serviceInstanceId.name())
                        .idleDuration(duration)
                        .excludeFromAutoEnrollment(excludePattern)
                        .build()));
        nbServicesInit = countServices();

    }

    @Test
    public void testInsert() {
        dao.save(SpaceEnrollerConfig.builder().id("testInsertServiceSuccess").build());
        assertThat(countServices(), is(equalTo(nbServicesInit + 1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() throws ServiceInstanceExistsException, ServiceBrokerException {
        List<String> ids = Arrays.asList("testInsertServiceSuccess1", "testInsertServiceSuccess2");
        List<SpaceEnrollerConfig> initialList = ids.stream()
                .map(id -> SpaceEnrollerConfig.builder().id(id).build())
                .collect(Collectors.toList());


        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the initial amount plus inserted", countServices(), is(equalTo(
                nbServicesInit + initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<SpaceEnrollerConfig> storedElement = dao.findAll();
        int count = 0;
        for (SpaceEnrollerConfig ignored : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == nbServicesInit + initialList
                .size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        log.debug("initial list {}", initialList.size());
        for (SpaceEnrollerConfig object : initialList) {
            log.debug("initial object {} ", object.getId());
        }
        for (SpaceEnrollerConfig object : storedElement) {
            log.debug("found object {} , checking if in initial list", object.getId());
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }

    }

    @Test
    public void testGetServiceEquality() {
        String serviceId = "GetTest";
        SpaceEnrollerConfig originalService = SpaceEnrollerConfig.builder().id(serviceId).build();
        dao.save(originalService);
        SpaceEnrollerConfig serviceInstance = dao.findOne(serviceId);
        assertThat("Two objects should be equal", serviceInstance, is(equalTo(originalService)));
    }

    @Test
    public void testGetServiceByFields() {

        SpaceEnrollerConfig serviceInstance = dao.findOne(InsertedInstanceIds.testGetServiceSuccess.name());
        assertFalse("Service should have been found", serviceInstance == null);
        assertThat(serviceInstance.getId(), is(
                equalTo(InsertedInstanceIds.testGetServiceSuccess.name())));
        assertThat(serviceInstance.getPlanId(), is(equalTo(SERVICE_PLAN_ID)));
        assertThat(serviceInstance.getServiceDefinitionId(),
                is(equalTo(SERVICE_DEFINITION_ID)));
        assertThat(serviceInstance.getOrganizationId(), is(equalTo(ORG_TEST)));
        assertThat(serviceInstance.getSpaceId(), is(equalTo(SPACE_TEST)));
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(duration)));
        assertThat(serviceInstance.getExcludeFromAutoEnrollment().pattern(), is(equalTo(excludePattern.pattern())));
        assertTrue("Succeed in getting a service that does not exist", dao.findOne("testGetServiceFail") == null);

    }

    @Test
    public void testListServices() {
        assertThat(countServices(), is(equalTo(nbServicesInit)));
    }


    @Test
    public void testDelete() {

        //delete a service by id
        dao.delete(InsertedInstanceIds.testDeleteServiceByIdSuccess.name());
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(InsertedInstanceIds.testDeleteServiceByInstanceSuccess.name()));
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<SpaceEnrollerConfig> services = dao.findAll(Arrays.asList(InsertedInstanceIds.testDeleteMass01
                .name(), InsertedInstanceIds.testDeleteMass02.name()));
        dao.delete(services);
        assertThat(countServices(), is(equalTo(nbServicesInit - 4)));

        //delete all services
        dao.deleteAll();
        assertTrue(countServices() == 0);

    }


    private long countServices() {
        return dao.count();
    }
}