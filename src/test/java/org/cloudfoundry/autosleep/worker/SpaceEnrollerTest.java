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

package org.cloudfoundry.autosleep.worker;

import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.worker.remote.EntityNotFoundException;
import org.cloudfoundry.autosleep.worker.remote.EntityNotFoundException.EntityType;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpaceEnrollerTest {

    private static final String APP_ID = UUID.randomUUID().toString();

    private static final Duration INTERVAL = Duration.ofMillis(300);

    private static final String SERVICE_ID = "serviceId";

    private static final String SPACE_ID = UUID.randomUUID().toString();

    private String NEW_APP_ID = UUID.randomUUID().toString();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private Clock clock;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private DeployedApplicationConfig.Deployment deployment;

    private List<String> remoteApplicationIds = Arrays.asList(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            NEW_APP_ID,
            APP_ID);

    private SpaceEnroller spaceEnroller;

    @Mock
    private SpaceEnrollerConfig spaceEnrollerConfig;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    private <T> ArgumentMatcher<List<T>> anyListOfSize(final int expectedSize) {
        return new ArgumentMatcher<List<T>>() {

            @Override
            public boolean matches(Object object) {
                return List.class.isInstance(object) && List.class.cast(object).size() == expectedSize;
            }
        };
    }

    @Before
    public void buildMocks() throws CloudFoundryException {
        //default
        when(spaceEnrollerConfig.getSpaceId()).thenReturn(SPACE_ID);
        when(spaceEnrollerConfig.getId()).thenReturn(SERVICE_ID);

        when(deployment.getApplicationId()).thenReturn(APP_ID);

        spaceEnroller = spy(SpaceEnroller.builder()
                .clock(clock)
                .period(INTERVAL)
                .spaceEnrollerConfigId(SERVICE_ID)
                .spaceEnrollerConfigRepository(spaceEnrollerConfigRepository)
                .cloudFoundryApi(cloudFoundryApi)
                .applicationRepository(applicationRepository)
                .deployment(deployment)
                .build());
    }

    @Test
    public void test_enroller_bind_applications_bound_to_other_service_but_not_itself() throws Exception {
        //Given the service exist
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        //And it does not exlude any application
        when(spaceEnrollerConfig.getExcludeFromAutoEnrollment()).thenReturn(null);
        //And we localy have all remote bound to another service
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return local app id
                .filter(remoteApplicationId -> !remoteApplicationId.equals(APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfoLinkedToService(remoteApplicationId,
                        SERVICE_ID + "-other"))
                .collect(Collectors.toList()));
        //And remote applications contain the same applications
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), eq(null)))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> ApplicationIdentity.builder()
                                .guid(applicationId)
                                .name(applicationId)
                                .build())
                        .collect(Collectors.toList()));
        //When we run the task
        spaceEnroller.run();
        //Then it reschedule itself
        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
        //Normally all applications are bound
        verify(cloudFoundryApi, times(1))
                .bindServiceInstance(argThat(anyListOfSize(remoteApplicationIds.size() - 1)),
                        anyString());

    }

    @Test
    public void test_enroller_bind_new_application_but_not_itself() throws Exception {
        //Given the service exist
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        //And it does not exlude any application
        when(spaceEnrollerConfig.getExcludeFromAutoEnrollment()).thenReturn(null);
        //And we locally have all remote but the local one and another one
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return local app id
                .filter(remoteApplicationId -> !remoteApplicationId.equals(APP_ID)
                        && !remoteApplicationId.equals(NEW_APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfoLinkedToService(remoteApplicationId,
                        SERVICE_ID))
                .collect(Collectors.toList()));
        //And remote applications contain the all applications
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), eq(null)))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> ApplicationIdentity.builder()
                                .guid(applicationId)
                                .name(applicationId)
                                .build())
                        .collect(Collectors.toList()));
        //When we run the task
        spaceEnroller.run();
        //Then it reschedule itself
        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
        //Normally remote app has been bound
        verify(cloudFoundryApi, times(1))
                .bindServiceInstance(argThat(anyListOfSize(1)),
                        anyString());

    }

    @Test
    public void test_enroller_deletes_itself_when_service_does_not_exist_anymore() {
        //Given the service attached to tasks does not exist
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(null);
        //When task runs
        spaceEnroller.run();
        //Then it removes itself
        verify(clock, times(1)).removeTask(eq(SERVICE_ID));
        //And does not reschedule
        verify(spaceEnroller, never()).rescheduleWithDefaultPeriod();
    }

    @Test
    public void test_enroller_does_not_bind_itself_when_none_is_found_and_reschedule() throws Exception {
        //Given the service exist
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        //it will return every ids except local one
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return local app id
                .filter(remoteApplicationId -> !remoteApplicationId.equals(APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfoLinkedToService(remoteApplicationId,
                        SERVICE_ID))
                .collect(Collectors.toList()));
        //And remote applications contain the all applications
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> ApplicationIdentity.builder()
                                .guid(applicationId)
                                .name(applicationId)
                                .build())
                        .collect(Collectors.toList()));
        //When we run the task
        spaceEnroller.run();
        //Then it reschedule itself
        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
        //And it never bind an application
        verify(cloudFoundryApi, never())
                .bindServiceInstance(anyListOf(ApplicationIdentity.class), anyString());

    }

    @Test
    public void test_enroller_reschedule_itself_when_remote_error_occurs_on_binding()
            throws CloudFoundryException, EntityNotFoundException {
        //Given the service exist
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        //And local repository is empty
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        //And list of application returns some applications
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> ApplicationIdentity.builder()
                                .guid(applicationId)
                                .name(applicationId)
                                .build())
                        .collect(Collectors.toList()));
        //And binding will throw an error
        doThrow(new CloudFoundryException(
                new EntityNotFoundException(EntityType.service, SERVICE_ID)))
                .when(cloudFoundryApi)
                .bindServiceInstance(anyListOf(ApplicationIdentity.class), eq(SERVICE_ID));
        //When task is run
        spaceEnroller.run();
        //Then it rescheduled itself with default period
        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
    }

    @Test
    public void test_enroller_reschedule_itself_when_remote_error_occurs_on_remote_application_list()
            throws CloudFoundryException, EntityNotFoundException {
        //Given the service exist
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        //And local repository is empty
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        //And list of application will fail
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenThrow(new CloudFoundryException(null));
        //When task is run
        spaceEnroller.run();
        //Then it rescheduled itself with default period
        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
    }

}