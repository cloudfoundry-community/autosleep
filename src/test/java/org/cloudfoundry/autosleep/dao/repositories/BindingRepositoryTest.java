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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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

    private int countServices() {
        return toIntExact(dao.count());
    }

    @Test
    public void test_delete_all() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");

        Binding.BindingBuilder builder = Binding.builder()
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .serviceInstanceId("service");

        ids.forEach(id -> dao.save(builder.serviceBindingId(id).build()));

        //When all are deleted
        dao.deleteAll();

        //Then the returned count is null
        assertThat(countServices(), is(equalTo(0)));

    }

    @Test
    public void test_delete_by_entity_decrease_count() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");

        Binding.BindingBuilder builder = Binding.builder()
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .serviceInstanceId("service");

        ids.forEach(id -> dao.save(builder.serviceBindingId(id).build()));

        int nbServicesInit = ids.size();

        //When an entity is deleted by entity
        dao.delete(dao.findOne(ids.get(0)));

        //Then the count is decremented
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));
    }

    @Test
    public void test_delete_by_id_decrease_count() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");

        Binding.BindingBuilder builder = Binding.builder()
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .serviceInstanceId("service");

        ids.forEach(id -> dao.save(builder.serviceBindingId(id).build()));

        int nbServicesInit = ids.size();

        //When an entity is deleted by id
        dao.delete(ids.get(0));

        //Then the count is decremented
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));
    }

    @Test
    public void test_delete_in_mass() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");

        Binding.BindingBuilder builder = Binding.builder()
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .serviceInstanceId("service");

        ids.forEach(id -> dao.save(builder.serviceBindingId(id).build()));

        int nbServicesInit = ids.size();

        //When we request the deletion giving two existing ids
        Iterable<Binding> services = dao.findAll(Arrays.asList("deleteId1", "deleteId2"));
        dao.delete(services);

        //Then the count is decremented
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));
    }

    @Test
    public void test_empty_database_has_count_0() {
        assertThat(countServices(), is(equalTo(0)));
    }

    @Test
    public void test_find_by_resource_id_and_type_on_existing_type() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application);
        List<Binding> initialList = ids.stream()
                .map(id -> bindingBuilder.serviceBindingId(id).build())
                .collect(Collectors.toList());

        dao.save(initialList);
        //When we count element by resource and type that were inserted
        int count = dao.findByResourceIdAndType(Arrays.asList(APP_GUID, APP_GUID), ResourceType.Application).size();
        //then the count is equal to the one inserted
        assertTrue("Retrieving all elements should return the same quantity", count == initialList
                .size());
    }

    @Test
    public void test_find_by_resource_id_and_type_on_non_existing_type() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application);
        List<Binding> initialList = ids.stream()
                .map(id -> bindingBuilder.serviceBindingId(id).build())
                .collect(Collectors.toList());

        dao.save(initialList);
        //When we count element by resource and type that were not inserted
        int count = dao.findByResourceIdAndType(Collections.singletonList(APP_GUID), ResourceType.Route).size();
        //then the count is equal to zero
        assertTrue("No route binding should be found", count == 0);

    }

    @Test
    public void test_find_one_preserve_equality() {
        //Given an object is inserted
        String bindingId = "bidingIdEquality";
        String serviceId = "serviceIdEquality";
        Binding original = Binding.builder()
                .serviceBindingId(bindingId)
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .build();

        dao.save(original);

        //When we re load it
        Binding binding = dao.findOne(bindingId);

        //Then it is found
        assertFalse("Service binding should have been found", binding == null);
        //And all attrributes are equals to the one inserted
        assertThat(binding.getServiceInstanceId(), is(equalTo(serviceId)));
        assertThat(binding.getResourceId(), is(equalTo(APP_GUID)));
        assertThat(binding, is(equalTo(original)));

    }

    @Test
    public void test_find_one_returns_null_on_non_existing_id() {
        //Given an object is inserted
        String bindingId = "bidingIdEquality";
        String serviceId = "serviceIdEquality";
        Binding original = Binding.builder()
                .serviceBindingId(bindingId)
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .build();

        dao.save(original);

        //When we load a non existing id
        Binding binding = dao.findOne(bindingId + "-fail");
        //then the object returned is null
        assertThat("Succeed in getting a binding that does not exist", binding, is(nullValue()));
    }

    @Test
    public void test_findall_filtered() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application);
        List<Binding> initialList = ids.stream()
                .map(id -> bindingBuilder.serviceBindingId(id).build())
                .collect(Collectors.toList());

        dao.save(initialList);
        dao.save(bindingBuilder.serviceBindingId("testInsert3").build());

        //when we iterate on filtered objects
        //then we have objects that existed
        int count = 0;
        for (Binding object : dao.findAll(ids)) {
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
            count++;
        }
        //and  we find the good number of objects
        assertTrue("Retrieving filtered elements should return the same quantity", count == initialList
                .size());

    }

    @Test
    public void test_findall_no_filter() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application);
        List<Binding> initialList = ids.stream()
                .map(id -> bindingBuilder.serviceBindingId(id).build())
                .collect(Collectors.toList());

        dao.save(initialList);

        //when we iterate on all objects
        int count = 0;
        for (Iterator<Binding> itElements = dao.findAll().iterator(); itElements.hasNext(); itElements.next() ) {
            count++;
        }
        //Then  we find the good number of objects
        assertTrue("Retrieving all elements should return the same quantity", count == initialList
                .size());

    }

    @Test
    public void test_insert_increase_count() {
        //Given there is no entity in database
        //When an entity is inserted
        dao.save(Binding.builder()
                .serviceBindingId("testInsert")
                .serviceInstanceId("testInsert")
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .build());
        //Then count is equal to one
        assertThat(countServices(), is(equalTo(1)));
    }

    @Test
    public void test_multiple_insert_increase_count() {
        //Given there is some entities in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application);
        List<Binding> initialList = ids.stream()
                .map(id -> bindingBuilder.serviceBindingId(id).build())
                .collect(Collectors.toList());

        dao.save(initialList);
        //When we request count
        //Then count is equal to number we inserted
        assertThat("Count should be equal to the amount inserted", countServices(),
                is(equalTo(initialList.size())));
    }

    @Test
    public void test_retrieve_by_id() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(serviceId)
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application);
        List<Binding> initialList = ids.stream()
                .map(id -> bindingBuilder.serviceBindingId(id).build())
                .collect(Collectors.toList());

        dao.save(initialList);

        //when we request each object by id
        //Then objects are found
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));
    }
}