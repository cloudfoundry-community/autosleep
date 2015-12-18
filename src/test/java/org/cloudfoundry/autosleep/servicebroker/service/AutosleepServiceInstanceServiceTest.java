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
    public void test_no_creation_accepted_when_already_exists() {
        //given the service already exists
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(true);
        //when instance created then an error is thrown
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), ServiceInstanceExistsException.class);
    }


    @Test
    public void test_no_creation_accepted_when_unknown_parameter() {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when an unknwon parameter is submitted
        String unknwonParameter = "unknownParameter";
        createRequest.setParameters(Collections.singletonMap(unknwonParameter, "unknownParameterValue"));

        //then an error is thrown
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(), is(equalTo(unknwonParameter))));
    }


    @Test
    public void test_creation_with_default() throws Exception {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when service is created
        ServiceInstance si = instanceService.createServiceInstance(createRequest);

        //then global watcher is invoked
        verify(globalWatcher, times(1)).watchServiceBindings(any(AutosleepServiceInstance.class),
                eq(Config.DELAY_BEFORE_FIRST_SERVICE_CHECK));
        //and no password encoded
        verify(passwordEncoder, never()).encode(anyString());
        //service is returned
        assertThat(si, is(notNullValue()));
        //and service is saved in database
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        AutosleepServiceInstance serviceInstance = serviceInstances.get(0);
        //and default values are applied
        assertFalse(serviceInstance.isForcedAutoEnrollment());
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(Config.DEFAULT_INACTIVITY_PERIOD)));
        assertThat(serviceInstance.getSecret(), is(nullValue()));
        assertThat(serviceInstance.getExcludeFromAutoEnrollment(), is(nullValue()));
    }


    @Test
    public void test_duration_is_well_read() throws Exception {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit only the duration
        createRequest.setParameters(singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PT10H"));
        instanceService.createServiceInstance(createRequest);


        //then  service is saved with good duration
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        AutosleepServiceInstance serviceInstance = serviceInstances.get(0);
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(Duration.ofHours(10))));
    }

    @Test
    public void test_exclusion_is_well_read() throws Exception {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit only the exclusion
        createRequest.setParameters(
                singletonMap(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, ".*"));
        instanceService.createServiceInstance(createRequest);

        //then  service is saved with good exclusion
        assertThat(serviceInstances.size(), is(equalTo(1)));
        AutosleepServiceInstance serviceInstance = serviceInstances.get(0);
        assertThat(serviceInstance.getExcludeFromAutoEnrollment().pattern(), is(equalTo(".*")));
    }

    @Test
    public void test_secret_is_well_read() throws Exception {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit only the secret
        createRequest.setParameters(singletonMap(Config.ServiceInstanceParameters.SECRET, "password"));
        ServiceInstance si = instanceService.createServiceInstance(createRequest);

        //then password is read and encoded
        verify(passwordEncoder, times(1)).encode(eq("password"));
        assertThat(si, is(notNullValue()));

        //and  service is saved with password encoded
        assertThat(serviceInstances.size(), is(equalTo(1)));
        AutosleepServiceInstance serviceInstance = serviceInstances.get(0);
        assertThat(serviceInstance.getSecret(), is(equalTo(passwordEncoded)));
    }

    @Test
    public void test_forced_auto_enrollment_fails_without_secret() throws Exception {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user only gives forced auto enrollment
        createRequest.setParameters(singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, Config
                .ServiceInstanceParameters.Enrollment.forced.name()));
        //then it fails with good parameter in the error
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));
    }


    @Test
    public void test_forced_auto_enrollment_succeeds_with_secret() throws Exception {
        //given the service does not exist
        when(serviceRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit forced auto enrollment and secret
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.forced.name());
        createRequest.setParameters(parameters);
        ServiceInstance si = instanceService.createServiceInstance(createRequest);

        //then service is saved with forced auto enrollment
        assertThat(si, is(notNullValue()));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        AutosleepServiceInstance serviceInstance = serviceInstances.get(0);
        assertTrue(serviceInstance.isForcedAutoEnrollment());
    }

    @Test
    public void test_get_service_instance_found() throws Exception {
        //given the repository contains a service
        service_exist_in_database();

        //when the get service is invoked
        ServiceInstance retrievedInstance = instanceService.getServiceInstance(SERVICE_INSTANCE_ID);

        //then the service is found
        assertThat(retrievedInstance, is(notNullValue()));
        //and valued with good id
        assertThat(retrievedInstance.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));
    }

    @Test
    public void test_get_service_instance_not_found() {
        //given the repository does not contain the service
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID))
                .thenReturn(null);
        ServiceInstance retrievedInstance = instanceService
                .getServiceInstance(
                        SERVICE_INSTANCE_ID);
        assertThat(retrievedInstance, is(nullValue()));
    }

    @Test
    public void test_update_on_non_existing() throws Exception {
        //given the repository does not contain the service
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        //when update is invoked the error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest),
                ServiceInstanceDoesNotExistException.class);
    }

    private AutosleepServiceInstance service_exist_in_database() {
        AutosleepServiceInstance existingServiceInstance = AutosleepServiceInstance.builder()
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .planId(PLAN_ID)
                .secret("secret")
                .forcedAutoEnrollment(true).build();

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(existingServiceInstance);
        return existingServiceInstance;
    }

    @Test
    public void test_update_fails_when_parameter_unknown() throws Exception {
        //given service exists
        service_exist_in_database();

        //when user gives an unknown parameter
        String unknwonParameter = "unknownParameter";
        updateRequest.setParameters(Collections.singletonMap(unknwonParameter, "someValue"));

        //then error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(), is(equalTo(unknwonParameter))));
    }

    @Test
    public void test_update_fails_when_parameter_cannot_be_updated() throws Exception {
        //given service exists
        service_exist_in_database();

        //when user gives a parameter that cannot be updated
        updateRequest.setParameters(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PT12M"));

        //then error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(),
                        is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION))));
    }

    @Test
    public void test_update_fails_when_autoenrollment_without_secret() throws Exception {
        //given service exists
        service_exist_in_database();

        //when user gives auto enrollment without secret
        updateRequest.setParameters(singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name()));

        //then error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(),
                        is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));
    }

    @Test
    public void test_update_succeeds_when_autoenrollment_with_secret() throws Exception {
        //given service exists and password matches
        final AutosleepServiceInstance existingServiceInstance = service_exist_in_database();
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        //when user gives auto enrollment with secret
        Map<String, Object> parameters = new HashMap<>();
        updateRequest.setParameters(parameters);
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        ServiceInstance si = instanceService.updateServiceInstance(updateRequest);

        //then service is updated
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));
        assertThat(si, is(notNullValue()));
        assertFalse(existingServiceInstance.isForcedAutoEnrollment());
    }

    @Test
    public void test_update_accept_secret_password() throws Exception {
        //given service exists and password matches
        service_exist_in_database();
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        //when user gives auto enrollment with super secret
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        parameters.put(Config.ServiceInstanceParameters.SECRET, superPassword);
        updateRequest.setParameters(parameters);

        //No exception should be raised, as the super-password was provided
        instanceService.updateServiceInstance(updateRequest);
    }


    @Test
    public void test_update_fails_when_change_plan_requested() throws Exception {

        //given service exists
        service_exist_in_database();

        //No change plan supported
        UpdateServiceInstanceRequest changePlanRequest = new UpdateServiceInstanceRequest(PLAN_ID + "_other",
                Collections.emptyMap())
                .withInstanceId(SERVICE_INSTANCE_ID);
        verifyThrown(() -> instanceService.updateServiceInstance(changePlanRequest),
                ServiceInstanceUpdateNotSupportedException.class);

    }

    @Test
    public void test_delete_service_instance() throws Exception {
        //given no application in repository
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        //when service is asked to delete
        ServiceInstance si = instanceService.deleteServiceInstance(deleteRequest);

        //then the repository is invoked
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        assertThat(si, is(notNullValue()));
        assertThat(si.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));

    }

    @Test
    public void test_applications_are_cleaned_when_service_deleted() throws Exception {
        //given application repository contains some application that reference ONLY the service
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

        //when delete is asked
        instanceService.deleteServiceInstance(deleteRequest);

        //then repository is invoked
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        //and info on applications are removed
        verify(applicationRepository, times(3)).delete(any(ApplicationInfo.class));
    }


    private Map<String, Object> singletonMap(String key, String value) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }


}