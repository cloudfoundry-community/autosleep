package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class AutosleepServiceInstanceServiceTest {

    private static final UUID ORG_TEST = UUID.randomUUID();

    private static final UUID SPACE_TEST = UUID.randomUUID();

    private static final UUID[] APP_TEST = {
            UUID.randomUUID(), UUID.randomUUID()
    };

    private static final String SERVICE_DEFINITION_ID = "serviceDefinitionId";

    private static final String PLAN_ID = "planId";

    private static final String SERVICE_INSTANCE_ID = "serviceInstanceId";

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private GlobalWatcher globalWatcher;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutoSleepServiceInstanceService instanceService;

    private CreateServiceInstanceRequest createRequest;

    private UpdateServiceInstanceRequest updateRequest;

    private DeleteServiceInstanceRequest deleteRequest;

    private String passwordEncoded = "passwordEncoded" ;

    @Before
    public void initService() {
        instanceService = new AutoSleepServiceInstanceService(applicationRepository, serviceRepository,
                globalWatcher, passwordEncoder);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn(passwordEncoded);


        createRequest = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID, PLAN_ID,
                ORG_TEST.toString(), SPACE_TEST.toString(), Collections.emptyMap());
        createRequest.withServiceInstanceId(SERVICE_INSTANCE_ID);

        updateRequest = new UpdateServiceInstanceRequest(PLAN_ID, Collections.emptyMap())
                .withInstanceId(SERVICE_INSTANCE_ID);

        deleteRequest = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID, SERVICE_DEFINITION_ID, PLAN_ID);

    }

    @Test
    public void testCreateServiceInstance() throws Exception {
        //test null request
        try {
            instanceService.createServiceInstance(null);
            log.debug("Service instance created");
            fail("Succeed in creating instanceService with no request");
        } catch (NullPointerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }
        //test existing instanceService request
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));
        try {
            instanceService.createServiceInstance(createRequest);
            fail("Succeed in creating an already existing instanceService");
        } catch (ServiceInstanceExistsException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        // Duration verification
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT10H"));
        ServiceInstance si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, never()).encode(anyString());
        assertThat(si, is(notNullValue()));

        verify(globalWatcher, times(1)).watchServiceBindings(SERVICE_INSTANCE_ID, Config.delayBeforeFirstServiceCheck);
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));

        assertThat(si, is(instanceOf(AutosleepServiceInstance.class)));
        AutosleepServiceInstance serviceInstance = (AutosleepServiceInstance) si;
        assertFalse(serviceInstance.isNoOptOut());
        assertThat(serviceInstance.getInterval(), is(equalTo(Duration.ofHours(10))));

        // Exclude names verification
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.EXCLUDE_PARAMETER, ".*"));
        si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, never()).encode(anyString());
        assertThat(si, is(notNullValue()));
        assertThat(si, is(instanceOf(AutosleepServiceInstance.class)));
        serviceInstance = (AutosleepServiceInstance) si;
        assertFalse(serviceInstance.isNoOptOut());
        assertThat(serviceInstance.getExcludeNames(), is(notNullValue()));
        assertThat(serviceInstance.getExcludeNames().pattern(), is(equalTo(".*")));

        // Secret verification
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.SECRET_PARAMETER, "password"));
        si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, times(1)).encode(eq("password"));
        assertThat(si, is(notNullValue()));
        assertThat(si, is(instanceOf(AutosleepServiceInstance.class)));
        serviceInstance = (AutosleepServiceInstance) si;
        assertFalse(serviceInstance.isNoOptOut());
        assertThat(serviceInstance.getSecretHash(), is(equalTo(passwordEncoded)));

        //No opt out verification
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AutosleepServiceInstance.SECRET_PARAMETER, "secret");
        parameters.put(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, "true");
        createRequest.setParameters(parameters);
        si = instanceService.createServiceInstance(createRequest);
        assertThat(si, is(notNullValue()));
        assertThat(si, is(instanceOf(AutosleepServiceInstance.class)));
        serviceInstance = (AutosleepServiceInstance) si;
        assertTrue(serviceInstance.isNoOptOut());
    }

    @Test
    public void testGetServiceInstance() throws Exception {
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));

        org.cloudfoundry.community.servicebroker.model.ServiceInstance retrievedInstance = instanceService
                .getServiceInstance(
                        SERVICE_INSTANCE_ID);
        assertThat(retrievedInstance, is(notNullValue()));
        assertThat(retrievedInstance.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));
    }

    @Test
    public void testUpdateServiceInstance() throws Exception {

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        try {
            instanceService.updateServiceInstance(updateRequest);
            fail("update not supposed to work on an unknown instanceService id");
        } catch (ServiceInstanceDoesNotExistException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AutosleepServiceInstance.SECRET_PARAMETER, "secret");
        parameters.put(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, Boolean.TRUE);
        createRequest.setParameters(parameters);
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));

        parameters.put(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, "false");
        UpdateServiceInstanceRequest changePlanRequest = new UpdateServiceInstanceRequest(PLAN_ID + "_other",
                parameters)
                .withInstanceId(SERVICE_INSTANCE_ID);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        try {
            instanceService.updateServiceInstance(changePlanRequest);
            fail("not supposed to be able to change plan");
        } catch (ServiceInstanceUpdateNotSupportedException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }

        parameters.put(AutosleepServiceInstance.SECRET_PARAMETER, "secret");
        parameters.put(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, "false");
        updateRequest.setParameters(parameters);
        ServiceInstance si = instanceService.updateServiceInstance(updateRequest);
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));
        assertThat(si, is(notNullValue()));
        assertThat(si, is(instanceOf(AutosleepServiceInstance.class)));
        assertFalse(((AutosleepServiceInstance) si).isNoOptOut());

    }

    @Test
    public void testDeleteServiceInstance() throws Exception {
        when(applicationRepository.findAll()).thenReturn(Arrays.asList());
        ServiceInstance si = instanceService.deleteServiceInstance(deleteRequest);
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        assertThat(si, is(notNullValue()));
        assertThat(si.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));

    }

    @Test
    public void testCleanAppOnDeleteServiceInstance() throws Exception {
        //mocking app repository so that it return 3 apps linked to the service and 2 linked to others
        when(applicationRepository.findAll()).thenReturn(Arrays.asList(
                        new ApplicationInfo(UUID.randomUUID(), SERVICE_INSTANCE_ID),
                        new ApplicationInfo(UUID.randomUUID(), SERVICE_INSTANCE_ID),
                        new ApplicationInfo(UUID.randomUUID(), SERVICE_INSTANCE_ID),
                        new ApplicationInfo(UUID.randomUUID(), "àç!àpoiu"),
                        new ApplicationInfo(UUID.randomUUID(), "lkv nàç "))
        );

        instanceService.deleteServiceInstance(deleteRequest);
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        verify(applicationRepository, times(3)).delete(any(ApplicationInfo.class));
    }

    @Test
    public void testProcessSecretFailures() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.SECRET_PARAMETER, "secret"));
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));
        try {
            instanceService.updateServiceInstance(updateRequest);
            fail("Update should have been impossible due to no secret provided");
        } catch (InvalidParameterException i) {
            assertThat(i.getParameterName(), is(equalTo(AutosleepServiceInstance.SECRET_PARAMETER)));
        }
        updateRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.SECRET_PARAMETER, "secret"));
        when(passwordEncoder.matches(any(CharSequence.class), anyString())).thenReturn(false);
        try {
            instanceService.updateServiceInstance(updateRequest);
            fail("Update should have been impossible due to non matching password");
        } catch (InvalidParameterException i) {
            assertThat(i.getParameterName(), is(equalTo(AutosleepServiceInstance.SECRET_PARAMETER)));
        }
    }

    @Test
    public void testProcessInactivityFailures() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PP"));
        try {
            instanceService.createServiceInstance(createRequest);
            fail("Create should have been impossible due to invalid inactivity");
        } catch (InvalidParameterException i) {
            assertThat(i.getParameterName(), is(equalTo(AutosleepServiceInstance.INACTIVITY_PARAMETER)));
        }
    }


    @Test
    public void testProcessExcludeNamesFailure() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.EXCLUDE_PARAMETER, "*"));
        try {
            instanceService.createServiceInstance(createRequest);
            fail("Create should have been impossible due to invalid exclude pattern");
        } catch (InvalidParameterException i) {
            assertThat(i.getParameterName(), is(equalTo(AutosleepServiceInstance.EXCLUDE_PARAMETER)));
        }
    }

    @Test
    public void testProcessNoOptOutFailure() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, "true"));
        try {
            instanceService.createServiceInstance(createRequest);
            fail("Create should have been impossible due to no secret provided");
        } catch (InvalidParameterException i) {
            assertThat(i.getParameterName(), is(equalTo(AutosleepServiceInstance.NO_OPTOUT_PARAMETER)));
        }
    }

}