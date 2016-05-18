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

package org.cloudfoundry.autosleep.ui.web.controller;

import org.cloudfoundry.autosleep.ui.web.controller.ApiController;
import org.cloudfoundry.autosleep.ui.web.controller.DashboardController;
import org.cloudfoundry.autosleep.ui.web.controller.DebugController;
import org.cloudfoundry.autosleep.access.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.SpaceEnrollerConfigRepository;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DebugControllerTest {

    private static final String serviceInstanceId = "id";

    @InjectMocks
    private ApiController apiController;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private BindingRepository bindingRepo;

    @Mock
    private Catalog catalog;

    @InjectMocks
    private DashboardController dashboardController;

    @InjectMocks
    private DebugController debugController;

    private MockMvc mockMvc;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(debugController).build();
        when(catalog.getServiceDefinitions()).thenReturn(Collections.singletonList(
                new ServiceDefinition("serviceDefinitionId", "serviceDefinition", "", true,
                        Collections.singletonList(new Plan("planId", "plan", "")))));

    }

    @Test
    public void test_list_applications_page() throws Exception {
        //Given nothing
        //When a request is done on list applications page
        ResultActions resultActions = mockMvc.perform(get("/admin/debug/applications/")
                .accept(MediaType.TEXT_HTML));
        //Then status is OK
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void test_list_bindings_page() throws Exception {
        //Given nothing
        //When a request is done on list service bindings page
        ResultActions resultActions = mockMvc.perform(get("/admin/debug/" + serviceInstanceId + "/bindings/")
                .accept(MediaType.TEXT_HTML));
        //Then status is OK
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void test_list_instances_page() throws Exception {
        //Given nothing
        //When a request is done on list service instances page
        ResultActions resultActions = mockMvc.perform(get("/admin/debug/")
                .accept(MediaType.TEXT_HTML));
        //Then status is OK
        resultActions.andExpect(status().isOk());
    }

}