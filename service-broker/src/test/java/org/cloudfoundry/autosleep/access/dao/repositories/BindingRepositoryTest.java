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
import org.cloudfoundry.autosleep.access.dao.config.RepositoryConfig;
import org.cloudfoundry.autosleep.access.dao.model.Binding;
import org.cloudfoundry.autosleep.access.dao.model.Binding.ResourceType;
import org.cloudfoundry.autosleep.access.dao.repositories.CrudRepositoryTest;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class BindingRepositoryTest extends CrudRepositoryTest<Binding> {

    private static final String APP_GUID = "2F5A0947-6468-401B-B12A-963405121937";

    @Autowired
    private BindingRepository bindingRepository;

    @Override
    protected Binding build(String id) {
        return Binding.builder()
                .resourceId(APP_GUID)
                .resourceType(ResourceType.Application)
                .serviceInstanceId("service")
                .serviceBindingId(id)
                .build();
    }

    @Override
    protected void compareReloaded(Binding original, Binding reloaded) {
        assertThat(reloaded.getServiceInstanceId(), is(equalTo(original.getServiceInstanceId())));
        assertThat(reloaded.getResourceId(), is(equalTo(original.getResourceId())));
        assertThat(reloaded, is(equalTo(original)));
    }

    /**
     * Init DAO with test data.
     */
    @Before
    @After
    public void setAndClearDao() {
        setDao(bindingRepository);
        bindingRepository.deleteAll();
    }

    @Test
    public void test_find_by_resource_id_and_type_on_existing_type() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testFind1", "testFind2");
        ids.forEach(id -> bindingRepository.save(build(id)));
        //When we count element by resource and type that were inserted
        int count = bindingRepository.findByResourceIdAndType(Arrays.asList(APP_GUID, APP_GUID),
                ResourceType.Application).size();
        //then the count is equal to the one inserted
        assertTrue("Retrieving all elements should return the same quantity", count == ids.size());
    }

    @Test
    public void test_find_by_resource_id_and_type_on_non_existing_type() {
        //Given there is some entity in database
        List<String> ids = Arrays.asList("testFind1", "testFind2");
        ids.forEach(id -> bindingRepository.save(build(id)));
        //When we count element by resource and type that were not inserted
        int count = bindingRepository.findByResourceIdAndType(Collections.singletonList(APP_GUID),
                ResourceType.Route).size();
        //then the count is equal to zero
        assertTrue("No route binding should be found", count == 0);
    }

}