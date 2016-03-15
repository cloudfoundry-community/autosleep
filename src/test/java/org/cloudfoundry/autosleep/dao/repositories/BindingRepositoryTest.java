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
import org.cloudfoundry.autosleep.dao.model.Binding;
import org.cloudfoundry.autosleep.dao.model.Binding.ResourceType;
import org.cloudfoundry.autosleep.util.ApplicationConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.toIntExact;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class BindingRepositoryTest {

    private static final String APP_GUID = "2F5A0947-6468-401B-B12A-963405121937";

    @Autowired
    private BindingRepository dao;

    /**
     * Init DAO with test data.
     */
    @Before
    @After
    public void clearDao() {
        dao.deleteAll();
    }

    @Test
    public void testInsert() {
        dao.save(Binding.builder()
                .serviceBindingId("testInsert")
                .serviceInstanceId("testInsert")
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .build());
        assertThat(countServices(), is(equalTo(1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() {
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        List<Binding> initialList = new ArrayList<>();
        ids.forEach(id -> initialList.add(Binding.builder()
                .serviceBindingId(id)
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .build()));

        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the amount inserted", countServices(), is(equalTo(
                initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<Binding> storedElement = dao.findAll();
        int count = 0;
        for (Binding object : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == initialList
                .size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        for (Binding object : storedElement) {
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }


        count = dao.findByResourceIdAndType(Arrays.asList(APP_GUID,APP_GUID),ResourceType.Application).size();
        assertTrue("Retrieving all elements should return the same quantity", count == initialList
                .size());

        count = dao.findByResourceIdAndType(Collections.singletonList(APP_GUID),ResourceType.Route).size();
        assertTrue("No route binding should be found", count == 0);

    }

    @Test
    public void testEquality() {
        String bindingId = "bidingIdEquality";
        String serviceId = "serviceIdEquality";
        Binding original = Binding.builder()
                .serviceBindingId(bindingId)
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .build();

        dao.save(original);
        Binding binding = dao.findOne(bindingId);
        assertFalse("Service binding should have been found", binding == null);
        assertThat(binding.getServiceInstanceId(), is(equalTo(serviceId)));
        assertThat(binding.getResourceId(), is(equalTo(APP_GUID)));
        assertThat(binding, is(equalTo(original)));
        assertTrue("Succeed in getting a binding that does not exist", dao.findOne("testGetServiceFail") == null);

    }


    @Test
    public void testCount() {
        assertThat(countServices(), is(equalTo(0)));
    }


    @Test
    public void testDelete() {
        final String deleteByIdSuccess = "deleteByIdSuccess";
        final String deleteByInstanceSuccess = "deleteByInstanceSuccess";
        final String deleteByMass1 = "deleteByMass1";
        final String deleteByMass2 = "deleteByMass2";
        Binding.BindingBuilder builder = Binding.builder();
        builder.resourceId(APP_GUID).resourceType(ResourceType.Application).serviceInstanceId("service");
        dao.save(builder.serviceBindingId(deleteByIdSuccess).build());
        dao.save(builder.serviceBindingId(deleteByInstanceSuccess).build());
        dao.save(builder.serviceBindingId(deleteByMass1).build());
        dao.save(builder.serviceBindingId(deleteByMass2).build());

        int nbServicesInit = 4;
        assertThat(countServices(), is(equalTo(nbServicesInit)));

        //delete a service by binding id
        dao.delete(deleteByIdSuccess);
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(deleteByInstanceSuccess));
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<Binding> services = dao.findAll(Arrays.asList(deleteByMass1, deleteByMass2));
        dao.delete(services);
        assertThat(countServices(), is(equalTo(nbServicesInit - 4)));

        //delete all services
        dao.deleteAll();
        assertTrue(countServices() == 0);

    }

    private int countServices() {
        return toIntExact(dao.count());
    }
}