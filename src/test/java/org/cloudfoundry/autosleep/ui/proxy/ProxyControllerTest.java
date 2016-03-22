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

package org.cloudfoundry.autosleep.ui.proxy;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApi;
import org.cloudfoundry.autosleep.worker.scheduling.TimeManager;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState.STARTED;
import static org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState.STOPPED;
import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class ProxyControllerTest {

    static final String BINDING_ID = "someid";

    static final String ROUTE_ID = "routeid";

    @Mock
    private BindingRepository bindingRepo;

    @Mock
    private CloudFoundryApi cfApi;

    @Mock
    private TimeManager timeManager;

    private MockMvc mockMvc;

    @InjectMocks
    private ProxyController proxyController;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(proxyController).build();

    }

    @Test
    public void on_incoming_traffic_all_apps_stopped_linked_to_route_id_should_be_started() throws Exception {
        List<String> appList = Arrays.asList(randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString());

        //GIVEN that the binding is known
        when(bindingRepo.findOne(BINDING_ID)).thenReturn(BeanGenerator.createRouteBinding(BINDING_ID,
                "serviceid", ROUTE_ID));
        //and that stored route is linked to a list of application
        when(cfApi.listRouteApplications(ROUTE_ID)).thenReturn(appList);
        // and application are not yet started
        when(cfApi.startApplication(anyString())).thenReturn(true);
        //And they are start very quickly (ie. first get state will return started for each of them)
        when(cfApi.getApplicationState(anyString())).thenReturn(STARTED);

        //WHEN calling proxy path THEN traffic should be forwarded without any error
        mockMvc.perform(get(Path.PROXY_CONTEXT + "/" + BINDING_ID)
                .header(ProxyController.HEADER_FORWARD_URL, "www.cloudfoundry.org")
                .accept(MediaType.ALL))
                .andExpect(status().is(HttpStatus.FOUND.value()));

        //and all apps should be started
        verify(cfApi, times(appList.size())).startApplication(anyString());
        //and slept once to let application start
        verify(timeManager, times(1)).sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
        // and all application should be checked once
        verify(cfApi, times(appList.size())).getApplicationState(anyString());

    }

    @Test
    public void on_incoming_traffic_all_apps_already_started_does_not_check_and_return_immediately() throws Exception {
        List<String> appList = Arrays.asList(randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString());

        //GIVEN that the binding is known
        when(bindingRepo.findOne(BINDING_ID)).thenReturn(BeanGenerator.createRouteBinding(BINDING_ID,
                "serviceid", ROUTE_ID));
        //and that stored route is linked to a list of application
        when(cfApi.listRouteApplications(ROUTE_ID)).thenReturn(appList);
        // and application are not yet started
        when(cfApi.startApplication(anyString())).thenReturn(false);


        //WHEN calling proxy path THEN traffic should be forwarded without any error
        mockMvc.perform(get(Path.PROXY_CONTEXT + "/" + BINDING_ID)
                .header(ProxyController.HEADER_FORWARD_URL, "www.cloudfoundry.org")
                .accept(MediaType.ALL))
                .andExpect(status().is(HttpStatus.FOUND.value()));

        //and all apps should be started
        verify(cfApi, times(appList.size())).startApplication(anyString());
        //and since they are already started it should never sleep to let them start
        verify(timeManager, never()).sleep(any(Duration.class));
        //and it should never ask their state
        verify(cfApi, never()).getApplicationState(anyString());
    }

    @Test
    public void request_with_unknown_binding_only_forward_traffic() throws Exception {
        //GIVEN that the binding isn't known
        when(bindingRepo.findOne(BINDING_ID)).thenReturn(null);

        //WHEN calling proxy path THEN traffic should be forwarded without any error
        mockMvc.perform(get(Path.PROXY_CONTEXT + "/" + BINDING_ID)
                .header(ProxyController.HEADER_FORWARD_URL, "www.cloudfoundry.org")
                .accept(MediaType.ALL))
                .andExpect(status().is(HttpStatus.FOUND.value()));
        //TODO test Spring ResultMatcher.redirectedUrlPattern
    }

    @Test
    public void request_without_forward_url_should_fail() throws Exception {
        verifyThrown(() -> mockMvc.perform(get(Path.PROXY_CONTEXT + "/" + BINDING_ID)
                        .accept(MediaType.ALL))
                        .andExpect(status().is5xxServerError()),
                Exception.class);

    }
}