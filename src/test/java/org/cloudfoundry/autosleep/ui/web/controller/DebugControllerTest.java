package org.cloudfoundry.autosleep.ui.web.controller;

import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DebugControllerTest {

    private static final String serviceInstanceId = "id";

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BindingRepository bindingRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private Catalog catalog;

    @InjectMocks
    private DebugController debugController;

    @InjectMocks
    private ApiController apiController;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(debugController).build();
        when(catalog.getServiceDefinitions()).thenReturn(Collections.singletonList(
                new ServiceDefinition("serviceDefinitionId", "serviceDefinition", "", true,
                        Collections.singletonList(new Plan("planId", "plan", "")))));

    }

    @Test
    public void testInstances() throws Exception {
        mockMvc.perform(get("/admin/debug/")
                .accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    public void testBindings() throws Exception {
        mockMvc.perform(get("/admin/debug/" + serviceInstanceId + "/bindings/")
                .accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    public void testApplications() throws Exception {
        mockMvc.perform(get("/admin/debug/applications/")
                .accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }


}