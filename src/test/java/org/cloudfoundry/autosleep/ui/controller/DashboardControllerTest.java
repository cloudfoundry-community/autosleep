package org.cloudfoundry.autosleep.ui.controller;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DashboardControllerTest {

    private static final String serviceInstanceId = "dashboardCtrlTestSID";

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BindingRepository bindingRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private Catalog catalog;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    private static final String planId = "0309U";
    private static final String serviceDefinitionId = "serviceDefinitionId";

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
        when(catalog.getServiceDefinitions()).thenReturn(Collections.singletonList(
                new ServiceDefinition(serviceDefinitionId, "serviceDefinition", "", true,
                        Collections.singletonList(new Plan(planId, "plan", "")))));
    }

    private AutosleepServiceInstance getServiceInstance(boolean withExcludeParam) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AutosleepServiceInstance.INACTIVITY_PARAMETER, Duration.parse("PT1M"));
        parameters.put(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, true);
        parameters.put(AutosleepServiceInstance.SECRET_PARAMETER, "Pa$$w0rd");
        if (withExcludeParam) {
            parameters.put(AutosleepServiceInstance.EXCLUDE_PARAMETER, Pattern.compile(".*"));
        }
        CreateServiceInstanceRequest createRequest = new CreateServiceInstanceRequest(
                serviceDefinitionId, planId, "morg", "mySpace", parameters).withServiceInstanceId(serviceInstanceId);
        return new AutosleepServiceInstance(createRequest);
    }

    @Test
    public void testApps() throws Exception {
        when(serviceRepository.findOne(any())).thenReturn(getServiceInstance(false));

        mockMvc.perform(get(Config.Path.dashboardPrefix + serviceInstanceId).accept(MediaType.TEXT_HTML)).andExpect(
                status().isOk());

        when(serviceRepository.findOne(any())).thenReturn(getServiceInstance(true));
        mockMvc.perform(get(Config.Path.dashboardPrefix + serviceInstanceId).accept(MediaType.TEXT_HTML)).andExpect(
                status().isOk());
    }


}