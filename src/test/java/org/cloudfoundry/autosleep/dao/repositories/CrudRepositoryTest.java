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

import lombok.AccessLevel;
import lombok.Setter;
import org.junit.Test;
import org.springframework.data.repository.CrudRepository;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public abstract class CrudRepositoryTest<T> {

    @Setter(AccessLevel.PROTECTED)
    private CrudRepository<T, String> dao;

    protected abstract T build(String id);

    protected abstract void compareReloaded(T original, T reloaded);

    private int countEntities() {
        return toIntExact(dao.count());
    }

    @Test
    public void test_delete_all() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");
        ids.forEach(id -> dao.save(build(id)));

        //When all are deleted
        dao.deleteAll();

        //Then the returned count is null
        assertThat(countEntities(), is(equalTo(0)));
    }

    @Test
    public void test_delete_by_entity() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");
        ids.forEach(id -> dao.save(build(id)));

        //When an entity is deleted by entity
        dao.delete(dao.findOne(ids.get(0)));

        //Then the count is decremented
        assertThat(countEntities(), is(equalTo(ids.size() - 1)));
    }

    @Test
    public void test_delete_by_id() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");
        ids.forEach(id -> dao.save(build(id)));

        //When an entity is deleted by id
        dao.delete(ids.get(0));

        //Then the count is decremented
        assertThat(countEntities(), is(equalTo(ids.size() - 1)));
    }

    @Test
    public void test_delete_in_mass() {
        //Given db has some ids
        List<String> ids = Arrays.asList("deleteId1", "deleteId2", "deleteId3", "deleteId4");

        ids.forEach(id -> dao.save(build(id)));

        //When we request the deletion giving two existing ids
        Iterable<T> entities = dao.findAll(ids.subList(0, 2));
        dao.delete(entities);

        //Then the count is decremented
        assertThat(countEntities(), is(equalTo(ids.size() - 2)));
    }

    @Test
    public void test_empty_database_has_count_0() {
        assertThat(countEntities(), is(equalTo(0)));
    }

    @Test
    public void test_exists() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        ids.forEach(id -> dao.save(build(id)));

        //When we request each object by id
        //Then objects are found
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));
    }

    @Test
    public void test_find_one_preserve_equality() {
        //Given an object is inserted
        String entityId = "someEntityId";
        T original = build(entityId);
        dao.save(original);

        //When we re load it
        T reloaded = dao.findOne(entityId);

        //Then it is found
        assertNotNull(reloaded);
        //And all attributes are equals to the one inserted
        compareReloaded(original, reloaded);
    }

    @Test
    public void test_find_one_returns_null_on_non_existing_id() {
        //Given an object is inserted
        String entityId = "someEntityId";
        T original = build(entityId);

        dao.save(original);

        //When we load a non existing id
        T other = dao.findOne(entityId + "-fail");
        //then the object returned is null
        assertThat(other, is(nullValue()));
    }

    @Test
    public void test_findall_filtered() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testFindAll1", "testFindAll2");
        dao.save(ids.stream()
                .map(this::build)
                .collect(Collectors.toList()));
        dao.save(build("testFindAll3"));

        //when we iterate on filtered objects
        //then we have objects that existed
        int count = 0;
        for (Iterator<T> itElements = dao.findAll(ids).iterator(); itElements.hasNext(); itElements.next()) {
            count++;
        }
        //and  we find the good number of objects
        assertThat("Retrieving filtered elements should return the same quantity", count,
                is(equalTo(ids.size())));

    }

    @Test
    public void test_findall_no_filter() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testFindAll1", "testFindAll2");
        dao.save(ids.stream()
                .map(this::build)
                .collect(Collectors.toList()));
        //when we iterate on all objects
        int count = 0;
        for (Iterator<T> itElements = dao.findAll().iterator(); itElements.hasNext(); itElements.next()) {
            count++;
        }
        //Then  we find the good number of objects
        assertThat("Retrieving all elements should return the same quantity", count, is(equalTo(ids.size())));
    }

    @Test
    public void test_insert() {
        //Given there is no entity in database

        //When an entity is inserted
        dao.save(build("testInsert"));
        //Then count is equal to one
        assertThat(countEntities(), is(equalTo(1)));
    }

    @Test
    public void test_multiple_inserts() {
        //Given there is no entities in database
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        List<T> initialList = ids.stream()
                .map(this::build)
                .collect(Collectors.toList());

        //When we insert multiple entities
        dao.save(initialList);

        //Then count is equal to number we inserted
        assertThat("Count should be equal to the amount inserted", countEntities(),
                is(equalTo(initialList.size())));
    }

}
