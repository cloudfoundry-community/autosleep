/**
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

package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReaderFactory;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceResponse;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceResponse;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceResponse;
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

    private static final String ORG_TEST = UUID.randomUUID().toString();

    private static final String PLAN_ID = "planId";

    private static final String SERVICE_DEFINITION_ID = "serviceDefinitionId";

    private static final String SERVICE_INSTANCE_ID = "id";

    private static final String SPACE_TEST = UUID.randomUUID().toString();

    @Mock
    private ApplicationLocker applicationLocker;

    @Mock
    private ApplicationRepository applicationRepository;

    private DeleteServiceInstanceRequest deleteRequest;
    private CreateServiceInstanceRequest createRequest;

    @Mock
    private DeployedApplicationConfig.Deployment deployment;

    @Mock
    private Environment environment;

    @InjectMocks
    private AutosleepServiceInstanceService instanceService;

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

    private String passwordEncoded = "passwordEncoded";

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    @Qualifier(Config.ServiceInstanceParameters.SECRET)
    private ParameterReader<String> secretReader = parameterReaderFactory.buildSecretReader();

    private List<SpaceEnrollerConfig> serviceInstances = new ArrayList<>();

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    private String superPassword = "MEGAPASS";

    @Mock
    private WorkerManagerService workerManager;

    private CreateServiceInstanceRequest getCreateRequestWithArbitraryParams(Map<String, Object> params) {
        if (params == null) {
            params = Collections.emptyMap();
        }
        return new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID, PLAN_ID,
                ORG_TEST, SPACE_TEST, params, false).withServiceInstanceId(SERVICE_INSTANCE_ID);
    }

    private UpdateServiceInstanceRequest getUpdateRequestWithArbitraryParams(Map<String, Object> params) {
        if (params == null) {
            params = Collections.emptyMap();
        }
        return new UpdateServiceInstanceRequest(SERVICE_DEFINITION_ID, PLAN_ID,
                params,
                false)
                .withServiceInstanceId(SERVICE_INSTANCE_ID);
    }

    @Test
    public void arbitrary_duration_is_stored() throws Exception {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit only the duration
        Map<String, Object> params = singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PT10H");
        CreateServiceInstanceRequest createRequest = getCreateRequestWithArbitraryParams(params);
        instanceService.createServiceInstance(createRequest);

        //then  service is saved with good duration
        verify(spaceEnrollerConfigRepository, times(1)).save(any(SpaceEnrollerConfig.class));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        SpaceEnrollerConfig serviceInstance = serviceInstances.get(0);
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(Duration.ofHours(10))));
    }

    @Before
    public void initService() {
        serviceInstances.clear();
        doAnswer(invocationOnMock ->
                        serviceInstances.add((SpaceEnrollerConfig) invocationOnMock.getArguments()[0])
        ).when(spaceEnrollerConfigRepository).save(any(SpaceEnrollerConfig.class));
        doAnswer(invocationOnMock -> {
            ((Runnable) invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn(passwordEncoded);

        deleteRequest = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID, SERVICE_DEFINITION_ID, PLAN_ID, null,
                false);

        createRequest = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID, PLAN_ID,
                ORG_TEST, SPACE_TEST, null, false);
        createRequest.withServiceInstanceId(SERVICE_INSTANCE_ID);
     
        when(environment.getProperty(Config.EnvKey.SECURITY_PASSWORD)).thenReturn(superPassword);

    }

    private SpaceEnrollerConfig service_exist_in_database() {
        SpaceEnrollerConfig existingServiceInstance = SpaceEnrollerConfig.builder()
                .id(SERVICE_INSTANCE_ID)
                .planId(PLAN_ID)
                .secret("secret")
                .forcedAutoEnrollment(true).build();

        when(spaceEnrollerConfigRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(existingServiceInstance);
        return existingServiceInstance;
    }

    private Map<String, Object> singletonMap(String key, String value) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }



    @Test
    public void test_applications_are_cleaned_when_service_deleted() throws Exception {
        //given application repository contains some application that reference ONLY the service
        Map<String, ApplicationInfo> applicationInfos = Arrays.asList(
                BeanGenerator.createAppInfoLinkedToService(SERVICE_INSTANCE_ID),
                BeanGenerator.createAppInfoLinkedToService(SERVICE_INSTANCE_ID),
                BeanGenerator.createAppInfoLinkedToService(SERVICE_INSTANCE_ID),
                BeanGenerator.createAppInfoLinkedToService("àç!àpoiu"),
                BeanGenerator.createAppInfoLinkedToService("lkv nàç ")
        ).stream().collect(Collectors.toMap(ApplicationInfo::getUuid,
                applicationInfo -> applicationInfo));
        when(applicationRepository.findAll()).thenReturn(applicationInfos.values());

        when(applicationRepository.findOne(anyString()))
                .then(invocationOnMock -> applicationInfos.get((String) invocationOnMock.getArguments()[0]));

        when(spaceEnrollerConfigRepository.findOne(anyString())).thenReturn(BeanGenerator.createServiceInstance());

        //when delete is asked
        instanceService.deleteServiceInstance(deleteRequest);

        //then repository is invoked
        verify(spaceEnrollerConfigRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        //and info on applications are removed
        verify(applicationRepository, times(3)).delete(any(ApplicationInfo.class));
    }

    @Test
    public void test_creation_with_default() throws Exception {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when service is created
        CreateServiceInstanceResponse si =
                instanceService.createServiceInstance(getCreateRequestWithArbitraryParams(null));

        //then global watcher is invoked
        verify(workerManager, times(1)).registerSpaceEnroller(any(SpaceEnrollerConfig.class));
        //and no password encoded
        verify(passwordEncoder, never()).encode(anyString());
        //service is returned
        assertThat(si, is(notNullValue()));
        //and service is saved in database
        verify(spaceEnrollerConfigRepository, times(1)).save(any(SpaceEnrollerConfig.class));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        SpaceEnrollerConfig serviceInstance = serviceInstances.get(0);
        //and default values are applied
        assertFalse(serviceInstance.isForcedAutoEnrollment());
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(Config.DEFAULT_INACTIVITY_PERIOD)));
        assertThat(serviceInstance.getSecret(), is(nullValue()));
        assertThat(serviceInstance.getExcludeFromAutoEnrollment(), is(nullValue()));
    }

    @Test
    public void test_delete_service_instance() throws Exception {
        //given no application in app repository, and one spaceEnrollerConfig
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(spaceEnrollerConfigRepository.findOne(anyString()))
                .thenReturn(BeanGenerator.createServiceInstance(SERVICE_INSTANCE_ID));

        //when service is asked to delete
        DeleteServiceInstanceResponse response = instanceService.deleteServiceInstance(deleteRequest);

        //then the repository is invoked
        verify(spaceEnrollerConfigRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void test_exclusion_is_well_read() throws Exception {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit only the exclusion
        Map<String, Object> params = singletonMap(
                Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, ".*");
        CreateServiceInstanceRequest createRequest = getCreateRequestWithArbitraryParams(params);
        instanceService.createServiceInstance(createRequest);

        //then  service is saved with good exclusion
        assertThat(serviceInstances.size(), is(equalTo(1)));
        SpaceEnrollerConfig serviceInstance = serviceInstances.get(0);
        assertThat(serviceInstance.getExcludeFromAutoEnrollment().pattern(), is(equalTo(".*")));
    }

    @Test
    public void test_forced_auto_enrollment_fails_without_secret() throws Exception {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user only gives forced auto enrollment
        Map<String, Object> params = singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, Config
                .ServiceInstanceParameters.Enrollment.forced.name());
        CreateServiceInstanceRequest createRequest = getCreateRequestWithArbitraryParams(params);
        //then it fails with good parameter in the error
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                exceptionThrown ->
                        assertThat(exceptionThrown.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));
    }

    @Test
    public void test_forced_auto_enrollment_succeeds_with_secret() throws Exception {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit forced auto enrollment and secret
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.forced.name());

        CreateServiceInstanceRequest createRequest = getCreateRequestWithArbitraryParams(parameters);
        CreateServiceInstanceResponse si = instanceService.createServiceInstance(createRequest);

        //then service is saved with forced auto enrollment
        assertThat(si, is(notNullValue()));
        assertThat(serviceInstances.size(), is(equalTo(1)));
        SpaceEnrollerConfig serviceInstance = serviceInstances.get(0);
        assertTrue(serviceInstance.isForcedAutoEnrollment());
    }

    @Test
    public void test_no_creation_accepted_when_already_exists() {
        //given the service already exists
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(true);
        //when instance created then an error is thrown
        verifyThrown(() -> instanceService.createServiceInstance(getCreateRequestWithArbitraryParams(null)),
                ServiceInstanceExistsException.class);
    }

    @Test
    public void test_no_creation_accepted_when_unknown_parameter() {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when an unknwon parameter is submitted
        String unknwonParameter = "unknownParameter";
        Map<String, Object> params = singletonMap(unknwonParameter, "unknownParameterValue");
        CreateServiceInstanceRequest createRequest = getCreateRequestWithArbitraryParams(params);

        //then an error is thrown
        verifyThrown(() -> instanceService.createServiceInstance(createRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(), is(equalTo(unknwonParameter))));
    }

    @Test
    public void test_secret_is_well_read() throws Exception {
        //given the service does not exist
        when(spaceEnrollerConfigRepository.exists(SERVICE_INSTANCE_ID)).thenReturn(false);

        //when user submit only the secret
        Map<String, Object> params = singletonMap(Config.ServiceInstanceParameters.SECRET, "password");
        CreateServiceInstanceRequest createRequest = getCreateRequestWithArbitraryParams(params);

        CreateServiceInstanceResponse si = instanceService.createServiceInstance(createRequest);

        //then password is read and encoded
        verify(passwordEncoder, times(1)).encode(eq("password"));
        assertThat(si, is(notNullValue()));

        //and  service is saved with password encoded
        assertThat(serviceInstances.size(), is(equalTo(1)));
        SpaceEnrollerConfig serviceInstance = serviceInstances.get(0);
        assertThat(serviceInstance.getSecret(), is(equalTo(passwordEncoded)));
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

        UpdateServiceInstanceRequest updateRequest = getUpdateRequestWithArbitraryParams(parameters);

        //No exception should be raised, as the super-password was provided
        instanceService.updateServiceInstance(updateRequest);
    }

    @Test
    public void test_update_fails_when_autoenrollment_without_secret() throws Exception {
        //given service exists
        service_exist_in_database();

        //when user gives auto enrollment without secret
        UpdateServiceInstanceRequest updateRequest = getUpdateRequestWithArbitraryParams(
                singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                        Config.ServiceInstanceParameters.Enrollment.standard.name()));

        //then error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(),
                        is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));
    }

    @Test
    public void test_update_fails_when_change_plan_requested() throws Exception {

        //given service exists
        service_exist_in_database();

        //No change plan supported
        UpdateServiceInstanceRequest changePlanRequest = new UpdateServiceInstanceRequest(SERVICE_DEFINITION_ID,
                PLAN_ID + "_other",
                Collections.emptyMap(),
                false)
                .withServiceInstanceId(SERVICE_INSTANCE_ID);

        verifyThrown(() -> instanceService.updateServiceInstance(changePlanRequest),
                ServiceInstanceUpdateNotSupportedException.class);

    }

    @Test
    public void test_update_fails_when_parameter_cannot_be_updated() throws Exception {
        //given service exists
        service_exist_in_database();

        //when user gives a parameter that cannot be updated
        //when user gives auto enrollment without secret
        UpdateServiceInstanceRequest updateRequest = getUpdateRequestWithArbitraryParams(
                singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PT12M"));

        //then error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(),
                        is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION))));
    }

    @Test
    public void test_update_fails_when_parameter_unknown() throws Exception {
        //given service exists
        service_exist_in_database();

        //when user gives an unknown parameter
        String unknownParameter = "unknownParameter";
        UpdateServiceInstanceRequest updateRequest = getUpdateRequestWithArbitraryParams(
                singletonMap(unknownParameter, "someValue"));

        //then error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest), InvalidParameterException.class,
                parameterChecked -> assertThat(parameterChecked.getParameterName(), is(equalTo(unknownParameter))));
    }

    @Test
    public void test_update_on_non_existing() throws Exception {
        //given the repository does not contain the service
        when(spaceEnrollerConfigRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        UpdateServiceInstanceRequest updateRequest = getUpdateRequestWithArbitraryParams(null);

        //when update is invoked the error is thrown
        verifyThrown(() -> instanceService.updateServiceInstance(updateRequest),
                ServiceInstanceDoesNotExistException.class);
    }

    @Test
    public void test_update_succeeds_when_autoenrollment_with_secret() throws Exception {
        //given service exists and password matches
        final SpaceEnrollerConfig existingServiceInstance = service_exist_in_database();
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        //when user gives auto enrollment with secret
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.SECRET, "secret");
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                Config.ServiceInstanceParameters.Enrollment.standard.name());
        UpdateServiceInstanceRequest updateRequest = getUpdateRequestWithArbitraryParams(parameters);

        UpdateServiceInstanceResponse response = instanceService.updateServiceInstance(updateRequest);

        //then service is updated
        verify(spaceEnrollerConfigRepository, times(1)).save(any(SpaceEnrollerConfig.class));
        assertThat(response, is(notNullValue()));
        assertFalse(existingServiceInstance.isForcedAutoEnrollment());
    }

}