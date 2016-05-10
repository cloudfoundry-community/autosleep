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

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.ui.web.controller.DashboardController;
import org.cloudfoundry.autosleep.access.dao.model.SpaceEnrollerConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DashboardControllerTest {

    private static final String planId = "0309U";

    private static final String serviceDefinitionId = "serviceDefinitionId";

    private static final String serviceInstanceId = "dashboardCtrlTestSID";

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private BindingRepository bindingRepository;

    @Mock
    private Catalog catalog;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    private SpaceEnrollerConfig getServiceInstance() {

        return SpaceEnrollerConfig.builder()
                .idleDuration(Duration.parse("PT1M"))
                .forcedAutoEnrollment(true)
                .secret("Pa$$w0rd")
                .serviceDefinitionId(serviceDefinitionId)
                .planId(planId)
                .organizationId("orgGuid")
                .spaceId("spaceId")
                .id(serviceInstanceId)
                .excludeFromAutoEnrollment(Pattern.compile(".*"))
                .build();
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
        when(catalog.getServiceDefinitions()).thenReturn(Collections.singletonList(
                new ServiceDefinition(serviceDefinitionId, "serviceDefinition", "", true,
                        Collections.singletonList(new Plan(planId, "plan", "")))));
    }

    @Test
    public void test_app_existing_id() throws Exception {
        //Given requested application exists
        when(spaceEnrollerConfigRepository.findOne(eq(serviceInstanceId)))
                .thenReturn(getServiceInstance());

        //When user performs a request
        ResultActions resultActions = mockMvc.perform(get(Config.Path.DASHBOARD_CONTEXT + "/" + serviceInstanceId)
                .accept(MediaType.TEXT_HTML));
        //Then status is ok
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void test_app_not_found() throws Exception {
        //Given requested application does not exist
        when(spaceEnrollerConfigRepository.findOne(eq(serviceInstanceId)))
                .thenReturn(null);

        //When user performs a request
        ResultActions resultActions = mockMvc.perform(get(Config.Path.DASHBOARD_CONTEXT + "/" + serviceInstanceId)
                .accept(MediaType.TEXT_HTML));

        //Then status is not found
        resultActions.andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

}