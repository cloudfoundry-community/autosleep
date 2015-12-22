package org.cloudfoundry.autosleep.ui.controller;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.mockito.Matchers.eq;
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

    private SpaceEnrollerConfig getServiceInstance(boolean withExcludeParam) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.IDLE_DURATION, Duration.parse("PT1M"));
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, true);
        parameters.put(Config.ServiceInstanceParameters.SECRET, "Pa$$w0rd");
        if (withExcludeParam) {
            parameters.put(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, Pattern.compile(".*"));
        }
        SpaceEnrollerConfig.SpaceEnrollerConfigBuilder builder = SpaceEnrollerConfig.builder()
                .idleDuration(Duration.parse("PT1M"))
                .forcedAutoEnrollment(true)
                .secret("Pa$$w0rd")
                .serviceDefinitionId(serviceDefinitionId)
                .planId(planId)
                .organizationId("orgGuid")
                .spaceId("spaceId")
                .serviceInstanceId(serviceInstanceId);
        if (withExcludeParam) {
            builder.excludeFromAutoEnrollment(Pattern.compile(".*"));
        }
        return builder.build();
    }

    @Test
    public void testApps() throws Exception {
        when(serviceRepository.findOne(eq(serviceInstanceId)))
                .thenReturn(getServiceInstance(false))
                .thenReturn(getServiceInstance(true))
                .thenReturn(null);

        mockMvc.perform(get(Config.Path.DASHBOARD_CONTEXT + "/" + serviceInstanceId).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(get(Config.Path.DASHBOARD_CONTEXT + "/" + serviceInstanceId).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(get(Config.Path.DASHBOARD_CONTEXT + "/" + serviceInstanceId).accept(MediaType.TEXT_HTML))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }


}