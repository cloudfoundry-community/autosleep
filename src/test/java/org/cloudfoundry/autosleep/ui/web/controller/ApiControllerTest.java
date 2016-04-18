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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.Binding;
import org.cloudfoundry.autosleep.dao.model.Binding.ResourceType;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.ui.web.model.ServerResponse;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class ApiControllerTest {

    private static final String applicationId = UUID.randomUUID().toString();

    private static final String serviceBindingId = "serviceBindingId";

    private static final String serviceInstanceId = "id";

    @InjectMocks
    private ApiController apiController;

    @Mock
    private ApplicationLocker applicationLocker;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private BindingRepository bindingRepository;

    @Mock
    private Catalog catalog;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    private void checkMvcResultsContainsASingleApplicationWithCorrectId(MvcResult mvcResult, String applicationId)
            throws UnsupportedEncodingException, IOException {
        ServerResponse<ApplicationInfo[]> applicationInfos = objectMapper
                .readValue(mvcResult.getResponse().getContentAsString(),
                        TypeFactory.defaultInstance()
                                .constructParametrizedType(ServerResponse.class, ServerResponse.class,
                                        ApplicationInfo[].class));
        assertThat(applicationInfos.getBody(), is(notNullValue()));
        assertThat(applicationInfos.getBody().length, is(equalTo(1)));
        assertThat(applicationInfos.getBody()[0].getUuid(), is(equalTo(applicationId)));
    }

    @Before
    public void init() {
        objectMapper = new ObjectMapper();
        //use this to help jackson using @JsonCreator annotation with multiple parameters
        objectMapper.registerModule(new ParameterNamesModule());
        doAnswer(invocationOnMock -> {
            ((Runnable) invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));
        mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
        when(catalog.getServiceDefinitions()).thenReturn(Collections.singletonList(
                new ServiceDefinition("serviceDefinitionId", "serviceDefinition", "", true,
                        Collections.singletonList(new Plan("planId", "plan", "")))));
    }

    @Test
    public void test_delete_application() throws Exception {
        //Given nothing
        String applicationId = "applicationToDelete";

        //When delete is done on an application
        ResultActions resultActions = mockMvc.perform(
                delete(Config.Path.API_CONTEXT + Config.Path.APPLICATIONS_SUB_PATH + applicationId));
        //Then NO Content is sent
        resultActions = resultActions.andExpect(status().is(HttpStatus.NO_CONTENT.value()));
        //AND it called delete on the repository
        resultActions.andDo(mvcResult -> verify(applicationRepository, times(1)).delete(eq(applicationId)));

    }

    @Test
    public void test_list_applications() throws Exception {
        //given repository contains an application black listed
        ApplicationInfo applicationInfo = BeanGenerator.createAppInfoWithDiagnostic(applicationId, "applicationName",
                CloudFoundryAppState.STARTED);
        applicationInfo.getEnrollmentState().addEnrollmentState("serviceId");
        applicationInfo.getEnrollmentState().updateEnrollment("serviceId", true);
        when(applicationRepository.findAll()).thenReturn(Collections.singletonList(applicationInfo));

        //When listing of application is called
        ResultActions resultActions = mockMvc.perform(
                get(Config.Path.API_CONTEXT + Config.Path.APPLICATIONS_SUB_PATH).accept(MediaType.APPLICATION_JSON));
        //Then result is ok
        resultActions = resultActions.andExpect(status().isOk());
        //And content type is application/json
        resultActions = resultActions.andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))));
        //And repository function was called
        resultActions = resultActions.andDo(mvcResult -> verify(applicationRepository, times(1)).findAll());
        //And result contains a single application with good id
        resultActions.andDo(mvcResult ->
                checkMvcResultsContainsASingleApplicationWithCorrectId(mvcResult, applicationId));
    }

    @Test
    public void test_list_applications_on_a_space_enroller_config() throws Exception {
        //Given a service is bound to an application
        String serviceId = "serviceIdListById";
        ApplicationInfo applicationInfo = BeanGenerator.createAppInfoWithDiagnostic(applicationId,
                "appName", CloudFoundryAppState.STARTED);
        applicationInfo.getEnrollmentState().addEnrollmentState(serviceId);
        when(applicationRepository.findAll()).thenReturn(Collections.singletonList(applicationInfo));

        //When list applications of this service
        ResultActions resultActions = mockMvc.perform(
                get(Config.Path.API_CONTEXT + Config.Path.SERVICES_SUB_PATH + serviceId + "/applications/")
                        .accept(MediaType.APPLICATION_JSON));
        // Then result is OK
        resultActions = resultActions.andExpect(status().isOk());
        //And content type is application/json
        resultActions = resultActions.andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))));
        //And repository function was called
        resultActions = resultActions.andDo(mvcResult -> verify(applicationRepository, times(1)).findAll());
        //And it contains a single application info with good id
        resultActions.andDo(mvcResult ->
                checkMvcResultsContainsASingleApplicationWithCorrectId(mvcResult, applicationId));
    }

    @Test
    public void test_list_bindings_on_exisiting_instance() throws Exception {
        //Given the repository contains a single binding of an instance
        Binding serviceBinding = Binding.builder()
                .serviceBindingId(serviceBindingId)
                .serviceInstanceId(serviceInstanceId)
                .resourceId(UUID.randomUUID().toString())
                .resourceType(ResourceType.Application)
                .build();
        when(bindingRepository.findAll()).thenReturn(Collections.singletonList(serviceBinding));

        //When list of the instance binding is called
        ResultActions resultActions = mockMvc.perform(
                get(Config.Path.API_CONTEXT + Config.Path.SERVICES_SUB_PATH + serviceInstanceId + "/bindings/")
                        .accept(MediaType.APPLICATION_JSON));

        //Then OK is sent
        resultActions = resultActions.andExpect(status().isOk());
        //And application/json content type is sent
        resultActions = resultActions.andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))));
        //And respository function is called
        resultActions = resultActions.andDo(mvcResult -> verify(bindingRepository, times(1)).findAll());
        //And the result contains a single service binding
        resultActions.andDo(mvcResult -> {
            ServerResponse<Binding[]> serviceBindings = objectMapper
                    .readValue(mvcResult.getResponse().getContentAsString(),
                            TypeFactory.defaultInstance()
                                    .constructParametrizedType(ServerResponse.class, ServerResponse.class,
                                            Binding[].class));
            assertThat(serviceBindings.getBody(), is(notNullValue()));
            assertThat(serviceBindings.getBody().length, is(equalTo(1)));
            assertThat(serviceBindings.getBody()[0].getServiceBindingId(), is(equalTo(serviceBindingId)));

        });
    }

    @Test
    public void test_list_bindings_on_non_exisitng_instance() throws Exception {
        //Given the repository does not contain any binding
        when(bindingRepository.findAll()).thenReturn(Collections.emptyList());
        //When list of bindings of unknown service binding is called
        ResultActions resultActions = mockMvc.perform(
                get(Config.Path.API_CONTEXT + Config.Path.SERVICES_SUB_PATH + serviceInstanceId + "-tmp" + "/bindings/")
                        .accept(MediaType.APPLICATION_JSON));
        //Then status OK is returned
        resultActions = resultActions.andExpect(status().isOk());
        //And application/json content type is returned
        resultActions = resultActions.andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))));
        //And respository function is called
        resultActions = resultActions.andDo(mvcResult -> verify(bindingRepository, times(1)).findAll());
        //And body contains an empty list
        resultActions.andDo(mvcResult -> {
            ServerResponse<Binding[]> serviceBindings = objectMapper
                    .readValue(mvcResult.getResponse().getContentAsString(),
                            TypeFactory.defaultInstance()
                                    .constructParametrizedType(ServerResponse.class, ServerResponse.class,
                                            Binding[].class));
            assertThat(serviceBindings.getBody(), is(notNullValue()));
            assertThat(serviceBindings.getBody().length, is(equalTo(0)));
        });
    }

    @Test
    public void test_list_space_enroller_config() throws Exception {
        //Given the repository contains a single config
        SpaceEnrollerConfig spaceEnrollerConfig = SpaceEnrollerConfig.builder()
                .id(serviceInstanceId).build();
        when(spaceEnrollerConfigRepository.findAll()).thenReturn(Collections.singletonList(spaceEnrollerConfig));

        //When request of list enroller config is done
        ResultActions resultActions = mockMvc.perform(
                get(Config.Path.API_CONTEXT + Config.Path.SERVICES_SUB_PATH).accept(MediaType.APPLICATION_JSON));

        //Then result is ok
        resultActions = resultActions.andExpect(status().isOk());
        //And content type is application/json
        resultActions = resultActions.andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))));
        //And repository function was called
        resultActions = resultActions.andDo(mvcResult -> verify(spaceEnrollerConfigRepository, times(1)).findAll());
        //And it contains a single config with good id
        resultActions.andDo(mvcResult -> {
            ServerResponse<SpaceEnrollerConfig[]> serviceInstances = objectMapper
                    .readValue(mvcResult.getResponse().getContentAsString(),
                            TypeFactory.defaultInstance()
                                    .constructParametrizedType(ServerResponse.class, ServerResponse.class,
                                            SpaceEnrollerConfig[].class)
                    );
            assertThat(serviceInstances.getBody(), is(notNullValue()));
            assertThat(serviceInstances.getBody().length, is(equalTo(1)));
            assertThat(serviceInstances.getBody()[0].getId(),
                    is(equalTo(serviceInstanceId)));

        });
    }

}