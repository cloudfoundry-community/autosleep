package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.autosleep.util.BeanGenerator;
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
import java.util.*;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class AutosleepServiceInstanceServiceTest {

    private static final UUID ORG_TEST = UUID.randomUUID();

    private static final UUID SPACE_TEST = UUID.randomUUID();

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

    @Mock
    private Deployment deployment;

    @InjectMocks
    private AutoSleepServiceInstanceService instanceService;

    private CreateServiceInstanceRequest createRequest;

    private UpdateServiceInstanceRequest updateRequest;

    private DeleteServiceInstanceRequest deleteRequest;

    private String passwordEncoded = "passwordEncoded";

    @Before
    public void initService() {
        instanceService = new AutoSleepServiceInstanceService(applicationRepository, serviceRepository,
                globalWatcher, passwordEncoder, deployment);
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
        verifyThrown(() -> instanceService.createServiceInstance(null), NullPointerException.class);

        //test existing instanceService request
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), ServiceInstanceExistsException.class);


        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        // Duration verification
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT10H"));
        ServiceInstance si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, never()).encode(anyString());
        assertThat(si, is(notNullValue()));

        verify(globalWatcher, times(1)).watchServiceBindings((AutosleepServiceInstance) si,
                Config.delayBeforeFirstServiceCheck);
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
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest),
                ServiceInstanceDoesNotExistException.class);

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
        verifyThrown(() -> instanceService.updateServiceInstance(changePlanRequest),
                ServiceInstanceUpdateNotSupportedException.class);

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
                BeanGenerator.createAppInfo(SERVICE_INSTANCE_ID),
                        BeanGenerator.createAppInfo(SERVICE_INSTANCE_ID),
                        BeanGenerator.createAppInfo(SERVICE_INSTANCE_ID),
                        BeanGenerator.createAppInfo("àç!àpoiu"),
                        BeanGenerator.createAppInfo("lkv nàç ")
        ));

        instanceService.deleteServiceInstance(deleteRequest);
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        verify(applicationRepository, times(3)).delete(any(ApplicationInfo.class));
    }

    @Test
    public void testProcessSecretFailures() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.SECRET_PARAMETER, "secret"));
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(AutosleepServiceInstance.SECRET_PARAMETER))));

        updateRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.SECRET_PARAMETER, "secret"));
        when(passwordEncoder.matches(any(CharSequence.class), anyString())).thenReturn(false);
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(AutosleepServiceInstance.SECRET_PARAMETER))));
    }

    @Test
    public void testProcessInactivityFailures() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PP"));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(AutosleepServiceInstance.INACTIVITY_PARAMETER))));
    }


    @Test
    public void testProcessExcludeNamesFailure() throws Exception {
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.EXCLUDE_PARAMETER, "*"));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(AutosleepServiceInstance.EXCLUDE_PARAMETER))));
    }

    @Test
    public void testProcessNoOptOutFailure() throws Exception {
        //No secret provided
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.NO_OPTOUT_PARAMETER, "true"));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(AutosleepServiceInstance.NO_OPTOUT_PARAMETER))));
    }

}