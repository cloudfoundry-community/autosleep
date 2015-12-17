package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.ApplicationLocker;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.autosleep.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.servicebroker.service.parameters.ParameterReaderFactory;
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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Mock
    private Environment environment;

    @Mock
    private ApplicationLocker applicationLocker;

    private ParameterReaderFactory parameterReaderFactory = new ParameterReaderFactory();

    @Spy
    @Qualifier(Config.ServiceInstanceParameters.IDLE_DURATION)
    private ParameterReader<Duration> idleDurationReader = parameterReaderFactory.buildIdleDurationReader();

    @Spy
    @Qualifier(Config.ServiceInstanceParameters.AUTO_ENROLLMENT)
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader
            = parameterReaderFactory.buildAutoEnrollmentReader();

    @Spy
    @Qualifier(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader
            = parameterReaderFactory.buildExcludeFromAutoEnrollmentReader();

    @Spy
    @Qualifier(Config.ServiceInstanceParameters.SECRET)
    private ParameterReader<String> secretReader = parameterReaderFactory.buildSecretReader();

    @InjectMocks
    private AutosleepServiceInstanceService instanceService;

    private CreateServiceInstanceRequest createRequest;

    private UpdateServiceInstanceRequest updateRequest;

    private DeleteServiceInstanceRequest deleteRequest;

    private String passwordEncoded = "passwordEncoded";

    private String superPassword = "MEGAPASS";


    private List<AutosleepServiceInstance> serviceInstances = new ArrayList<>();

    @Before
    public void initService() {
        serviceInstances.clear();
        doAnswer(invocationOnMock ->
                        serviceInstances.add((AutosleepServiceInstance) invocationOnMock.getArguments()[0])
        ).when(serviceRepository).save(any(AutosleepServiceInstance.class));
        doAnswer(invocationOnMock -> {
            ((Runnable) invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn(passwordEncoded);


        createRequest = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID, PLAN_ID,
                ORG_TEST.toString(), SPACE_TEST.toString(), Collections.emptyMap());
        createRequest.withServiceInstanceId(SERVICE_INSTANCE_ID);

        updateRequest = new UpdateServiceInstanceRequest(PLAN_ID, Collections.emptyMap())
                .withInstanceId(SERVICE_INSTANCE_ID);

        deleteRequest = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID, SERVICE_DEFINITION_ID, PLAN_ID);

        when(environment.getProperty(Config.EnvKey.SECURITY_PASSWORD)).thenReturn(superPassword);

    }

    @Test
    public void testCreateServiceInstance() throws Exception {
        //test null request
        verifyThrown(() -> instanceService.createServiceInstance(null), NullPointerException.class);

        //test existing instanceService request
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(true);
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), ServiceInstanceExistsException.class);


        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);
        // Duration verification
        createRequest.setParameters(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PT10H"));
        ServiceInstance si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, never()).encode(anyString());
        assertThat(si, is(notNullValue()));

        verify(globalWatcher, times(1)).watchServiceBindings(any(AutosleepServiceInstance.class),
                eq(Config.DELAY_BEFORE_FIRST_SERVICE_CHECK));
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));

        assertThat(serviceInstances.size(), is(equalTo(1)));
        AutosleepServiceInstance serviceInstance = serviceInstances.get(0);
        assertFalse(serviceInstance.isForcedAutoEnrollment());
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(Duration.ofHours(10))));

        // Exclude names verification
        serviceInstances.clear();
        createRequest.setParameters(
                Collections.singletonMap(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, ".*"));
        si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, never()).encode(anyString());
        assertThat(si, is(notNullValue()));

        assertThat(serviceInstances.size(), is(equalTo(1)));
        serviceInstance = serviceInstances.get(0);
        assertFalse(serviceInstance.isForcedAutoEnrollment());
        assertThat(serviceInstance.getExcludeFromAutoEnrollment(), is(notNullValue()));
        assertThat(serviceInstance.getExcludeFromAutoEnrollment().pattern(), is(equalTo(".*")));

        // Secret verification
        serviceInstances.clear();
        createRequest.setParameters(Collections.singletonMap(Config.ServiceInstanceParameters.SECRET, "password"));
        si = instanceService.createServiceInstance(createRequest);
        verify(passwordEncoder, times(1)).encode(eq("password"));
        assertThat(si, is(notNullValue()));

        assertThat(serviceInstances.size(), is(equalTo(1)));
        serviceInstance = serviceInstances.get(0);
        assertFalse(serviceInstance.isForcedAutoEnrollment());
        assertThat(serviceInstance.getSecret(), is(equalTo(passwordEncoded)));

        //Auto enrollment verification
        serviceInstances.clear();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.forced.name());
        createRequest.setParameters(parameters);
        si = instanceService.createServiceInstance(createRequest);
        assertThat(si, is(notNullValue()));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        serviceInstance = serviceInstances.get(0);
        assertTrue(serviceInstance.isForcedAutoEnrollment());
    }

    @Test
    public void testGetServiceInstance() throws Exception {
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID))
                .thenReturn(AutosleepServiceInstance.builder().serviceInstanceId(SERVICE_INSTANCE_ID).build());

        org.cloudfoundry.community.servicebroker.model.ServiceInstance retrievedInstance = instanceService
                .getServiceInstance(
                        SERVICE_INSTANCE_ID);
        assertThat(retrievedInstance, is(notNullValue()));
        assertThat(retrievedInstance.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));
        retrievedInstance = instanceService
                .getServiceInstance(
                        SERVICE_INSTANCE_ID + "-other");
        assertThat(retrievedInstance, is(nullValue()));
    }

    @Test
    public void testUpdateServiceInstance() throws Exception {

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest),
                ServiceInstanceDoesNotExistException.class);

        AutosleepServiceInstance existingServiceInstance = AutosleepServiceInstance.builder()
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .planId(PLAN_ID)
                .secret("secret")
                .forcedAutoEnrollment(true).build();

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(existingServiceInstance);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        UpdateServiceInstanceRequest changePlanRequest = new UpdateServiceInstanceRequest(PLAN_ID + "_other",
                parameters)
                .withInstanceId(SERVICE_INSTANCE_ID);
        verifyThrown(() -> instanceService.updateServiceInstance(changePlanRequest),
                ServiceInstanceUpdateNotSupportedException.class);

        parameters.clear();
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        updateRequest.setParameters(parameters);
        ServiceInstance si = instanceService.updateServiceInstance(updateRequest);
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));
        assertThat(si, is(notNullValue()));
        assertFalse(existingServiceInstance.isForcedAutoEnrollment());

    }

    @Test
    public void testDeleteServiceInstance() throws Exception {
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        ServiceInstance si = instanceService.deleteServiceInstance(deleteRequest);
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        assertThat(si, is(notNullValue()));
        assertThat(si.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));

    }

    @Test
    public void testCleanAppOnDeleteServiceInstance() throws Exception {
        //mocking app repository so that it return 3 apps linked to the service and 2 linked to others
        Map<String, ApplicationInfo> applicationInfos = Arrays.asList(
                BeanGenerator.createAppInfo(SERVICE_INSTANCE_ID),
                BeanGenerator.createAppInfo(SERVICE_INSTANCE_ID),
                BeanGenerator.createAppInfo(SERVICE_INSTANCE_ID),
                BeanGenerator.createAppInfo("àç!àpoiu"),
                BeanGenerator.createAppInfo("lkv nàç ")
        ).stream().collect(Collectors.toMap(applicationInfo -> applicationInfo.getUuid().toString(),
                applicationInfo -> applicationInfo));
        when(applicationRepository.findAll()).thenReturn(applicationInfos.values());

        when(applicationRepository.findOne(anyString()))
                .then(invocationOnMock -> applicationInfos.get((String) invocationOnMock.getArguments()[0]));
        instanceService.deleteServiceInstance(deleteRequest);
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        verify(applicationRepository, times(3)).delete(any(ApplicationInfo.class));
    }

    @Test
    public void testProcessSuperSecret() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        parameters.put(Config.ServiceInstanceParameters.SECRET, superPassword);
        updateRequest.setParameters(parameters);
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(AutosleepServiceInstance.builder()
                .planId(PLAN_ID)
                .serviceInstanceId(SERVICE_INSTANCE_ID).secret("secret").build());
        //No exception should be raised, as the super-password was provided
        instanceService.updateServiceInstance(updateRequest);
    }


    @Test
    public void testProcessSecretFailures() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        updateRequest.setParameters(parameters);

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(AutosleepServiceInstance.builder()
                .planId(PLAN_ID)
                .secret("secret")
                .serviceInstanceId(SERVICE_INSTANCE_ID).build());
        // no secret provided
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        updateRequest.setParameters(parameters);
        when(passwordEncoder.matches(any(CharSequence.class), anyString())).thenReturn(false);
        //does not match
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.SECRET))));
    }

    @Test
    public void testProcessInactivityFailures() throws Exception {
        createRequest.setParameters(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PP"));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION))));
    }


    @Test
    public void testProcessExcludeNamesFailure() throws Exception {
        createRequest.setParameters(
                Collections.singletonMap(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, "*"));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT))));
    }

    @Test
    public void testProcessNoOptOutFailure() throws Exception {
        //No secret provided
        createRequest.setParameters(Collections.singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, Config
                .ServiceInstanceParameters.Enrollment.forced.name()));
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));
    }

}