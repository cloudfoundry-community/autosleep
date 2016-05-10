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

package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo.EnrollmentState.State;
import org.cloudfoundry.autosleep.access.dao.model.Binding;
import org.cloudfoundry.autosleep.access.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.access.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.ServiceBindingResource;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.cloudfoundry.autosleep.access.dao.model.Binding.ResourceType.Application;
import static org.cloudfoundry.autosleep.access.dao.model.Binding.ResourceType.Route;
import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class AutosleepBindingServiceTest {

    private static final String APP_UID = UUID.randomUUID().toString();

    private static final String PLAN_ID = "planId";

    private static final String ROUTE_UID = UUID.randomUUID().toString();

    private static final String SERVICE_DEFINITION_ID = "serviceDefinitionId";

    @Mock
    private ApplicationRepository appRepo;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private ApplicationLocker applicationLocker;

    @Mock
    private BindingRepository bindingRepository;

    @InjectMocks
    private AutosleepBindingService bindingService;

    @Mock
    private CloudFoundryApiService cfApi;

    private CreateServiceInstanceBindingRequest createAppBindingTemplate;

    private CreateServiceInstanceBindingRequest createRouteBindingTemplate;

    @Mock
    private DeployedApplicationConfig.Deployment deployment;

    @Mock
    private ApplicationInfo.EnrollmentState enrollmentState;

    @Mock
    private SpaceEnrollerConfig spaceEnrollerConfig;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Mock
    private WorkerManagerService workerManager;

    @Test
    public void delete_app_binding_should_also_remove_route_binding_when_remote_unbind_fail() throws Exception {
        String testId = "testCascadeBindingDeletion";
        String linkedRouteBindingId = testId + "linkedRouteBinding";

        //given that a route is mapped in CF, also registered in autosleep
        final DeleteServiceInstanceBindingRequest deleteRequest = prepareCascadeDeleteTest(testId,
                linkedRouteBindingId);

        //and that a route is mapped to this application in CF
        when(cfApi.listApplicationRoutes(APP_UID)).thenReturn(singletonList(ROUTE_UID));
        //and that a route binding is also registered in autosleep
        when(bindingRepository.findByResourceIdAndType(singletonList(ROUTE_UID), Route))
                .thenReturn(singletonList(
                        BeanGenerator.createRouteBinding(linkedRouteBindingId, testId, ROUTE_UID)));
        //and for some reason cloudfoundry api will refuse route unbinding
        doThrow(new CloudFoundryException(new Throwable("TestException"))).when(cfApi).unbind(anyString());
        //when unbinding the app
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then app binding should be cleared from database, and CF API should be called to unbind route
        verify(appRepo, times(1)).delete(applicationInfo.getUuid());
        verify(bindingRepository, times(1)).delete(linkedRouteBindingId);
        verify(cfApi, times(1)).unbind(linkedRouteBindingId);

    }

    @Test
    public void delete_app_binding_should_also_unbind_route() throws Exception {
        String testId = "testCascadeBindingDeletion";
        String linkedRouteBindingId = testId + "linkedRouteBinding";

        //given that a route is mapped in CF, also registered in autosleep
        final DeleteServiceInstanceBindingRequest deleteRequest = prepareCascadeDeleteTest(testId,
                linkedRouteBindingId);

        //and that a route is mapped to this application in CF
        when(cfApi.listApplicationRoutes(APP_UID)).thenReturn(singletonList(ROUTE_UID));
        //and that a route binding is also registered in autosleep
        when(bindingRepository.findByResourceIdAndType(singletonList(ROUTE_UID), Route))
                .thenReturn(singletonList(
                        BeanGenerator.createRouteBinding(linkedRouteBindingId, testId, ROUTE_UID)));

        //when unbinding the app
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then app binding should be cleared from database
        verify(appRepo, times(1)).delete(applicationInfo.getUuid());
        //and CF API should be called to unbind route
        verify(cfApi, times(1)).unbind(linkedRouteBindingId);

    }

    @Test
    public void delete_app_binding_should_blacklist_app_if_autoenrollment_is_standard() throws Exception {
        final String bindingId = "testDelBinding";
        final String serviceId = "testDelBinding";

        //given that autoEnrollment is standard
        when(spaceEnrollerConfig.isForcedAutoEnrollment()).thenReturn(false);
        when(spaceEnrollerConfig.getId()).thenReturn(serviceId);

        //mock the effect of "updateEnrollment(serviceId,true);
        HashMap<String, State> services = new HashMap<>();
        services.put(serviceId, ApplicationInfo.EnrollmentState.State.BLACKLISTED);
        when(enrollmentState.getStates()).thenReturn(services);

        //when unbinding the app
        final DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppBindingTest(serviceId, bindingId);
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then it should be blacklisted and kept in database
        verify(enrollmentState, times(1)).updateEnrollment(eq(serviceId), eq(true));
        verify(appRepo, times(1)).save(applicationInfo);
        //while binding should be deleted
        verify(bindingRepository, times(1)).delete(bindingId);

    }

    @Test
    public void delete_app_binding_should_clear_app_if_autoenrollment_is_forced() throws Exception {
        String bindingId = "testDelBindingForcedEnrollment";
        String serviceId = "testDelBindingForcedEnrollment";
        final DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppBindingTest(serviceId, bindingId);

        //given that autoEnrollment is forced
        when(spaceEnrollerConfig.isForcedAutoEnrollment()).thenReturn(true);

        //when unbinding the app
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then it should be cleared from database
        verify(enrollmentState, times(1)).updateEnrollment(anyString(), eq(false));
        verify(appRepo, times(1)).delete(applicationInfo.getUuid());
        //while binding should be deleted
        verify(bindingRepository, times(1)).delete(bindingId);
    }

    @Test
    public void delete_app_binding_should_scream_if_unable_to_list_route_bindings() throws Exception {
        String testId = "testCascadeBindingDeletion";

        //given that a route is mapped in CF, also registered in autosleep
        final DeleteServiceInstanceBindingRequest deleteRequest = prepareCascadeDeleteTest(testId);
        //given that the cf api doesn't work as expected
        Mockito.doThrow(
                new CloudFoundryException(new Throwable("TestException"))
        ).when(cfApi).listApplicationRoutes(anyString());

        //when unbinding the app

        //then an exception should be raised
        verifyThrown(() -> bindingService.deleteServiceInstanceBinding(deleteRequest), ServiceBrokerException.class);
        verify(bindingRepository, never()).delete(testId);
    }

    /**
     * Init request templates with calaog definition, prepare mocks.
     */
    @Before
    public void init() {

        createAppBindingTemplate = new CreateServiceInstanceBindingRequest(SERVICE_DEFINITION_ID,
                PLAN_ID,
                null,
                Collections.singletonMap(ServiceBindingResource.BIND_RESOURCE_KEY_APP.toString(), APP_UID),
                null);

        createRouteBindingTemplate = new CreateServiceInstanceBindingRequest(SERVICE_DEFINITION_ID,
                PLAN_ID,
                null,
                Collections.singletonMap(ServiceBindingResource.BIND_RESOURCE_KEY_ROUTE.toString(), ROUTE_UID),
                null);

        when(applicationInfo.getUuid()).thenReturn(APP_UID);
        when(applicationInfo.getEnrollmentState()).thenReturn(enrollmentState);
        when(spaceEnrollerConfigRepository.findOne(any(String.class))).thenReturn(spaceEnrollerConfig);

        //avoir nullpointer when getting credentials
        when(spaceEnrollerConfig.getIdleDuration()).thenReturn(Duration.ofSeconds(10));

        doAnswer(invocationOnMock -> {
            ((Runnable) invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));

    }

    @Test
    public void new_app_binding_should_store_appinfo_binding_and_start_a_stopper() {
        //given that the application is unknown
        when(appRepo.findOne(APP_UID)).thenReturn(null);

        //when receive a new binding
        bindingService.createServiceInstanceBinding(createAppBindingTemplate.withServiceInstanceId("Sid")
                .withBindingId("Bid"));

        //then create it and store it to repository
        verify(appRepo, times(1)).save(any(ApplicationInfo.class));
        verify(bindingRepository, times(1)).save(any(Binding.class));
        verify(workerManager, times(1)).registerApplicationStopper(any(SpaceEnrollerConfig.class), anyString(),
                anyString());
    }

    @Test
    public void new_binding_raise_exception_if_unknow_routing_type() {

        //given that the request contains an unknown request type
        createRouteBindingTemplate = new CreateServiceInstanceBindingRequest(SERVICE_DEFINITION_ID, PLAN_ID, null,
                Collections.singletonMap("NorRouteNeitherApp", "à!"), null);

        //when receive a new binding

        //then it should scream, and not save anything
        verifyThrown(() -> bindingService.createServiceInstanceBinding(createRouteBindingTemplate
                .withServiceInstanceId("Sid")
                .withBindingId("Bidroute")), ServiceBrokerException.class);
        verify(bindingRepository, never()).save(any(Binding.class));
    }

    @Test
    public void new_route_binding_should_not_store_binding() throws Exception {
        //given that the app is linked to the route in CF
        when(cfApi.listRouteApplications(ROUTE_UID)).thenReturn(singletonList(APP_UID));
        //given that the application is known from autosleep
        when(appRepo.countByApplicationIds(singletonList(APP_UID))).thenReturn(1L);

        //when receive a new ROUTE binding
        bindingService.createServiceInstanceBinding(createRouteBindingTemplate.withServiceInstanceId("Sid")
                .withBindingId("Bidroute"));

        //then create it and store it to repository
        verify(appRepo, never()).save(any(ApplicationInfo.class));
        verify(bindingRepository, never()).save(any(Binding.class));

    }

    private DeleteServiceInstanceBindingRequest prepareCascadeDeleteTest(String testId, String linkedRouteBindingId)
            throws Exception {
        //given that a route is mapped to this application in CF
        when(cfApi.listApplicationRoutes(APP_UID)).thenReturn(singletonList(ROUTE_UID));
        //given that a route binding is also registered in autosleep
        when(bindingRepository.findByResourceIdAndType(singletonList(ROUTE_UID), Route))
                .thenReturn(singletonList(
                        BeanGenerator.createRouteBinding(linkedRouteBindingId, testId, ROUTE_UID)));

        return prepareDeleteAppBindingTest(testId, testId);
    }

    private DeleteServiceInstanceBindingRequest prepareCascadeDeleteTest(String testId) throws Exception {
        String linkedRouteBindingId = testId + "linkedRouteBinding";
        return prepareCascadeDeleteTest(testId, linkedRouteBindingId);
    }

    private DeleteServiceInstanceBindingRequest prepareDeleteAppBindingTest(String serviceId, String bindingId) {

        when(appRepo.findOne(APP_UID)).thenReturn(applicationInfo);
        when(bindingRepository.findOne(bindingId))
                .thenReturn(Binding.builder().serviceBindingId(bindingId)
                        .resourceType(Application)
                        .serviceInstanceId(serviceId)
                        .resourceId(APP_UID).build());
        return new DeleteServiceInstanceBindingRequest(serviceId, bindingId, SERVICE_DEFINITION_ID, PLAN_ID, null);

    }

    private DeleteServiceInstanceBindingRequest prepareDeleteAppRouteTest(String serviceId, String bindingId, String
            routeId) {

        when(appRepo.findOne(APP_UID)).thenReturn(applicationInfo);
        when(bindingRepository.findOne(bindingId))
                .thenReturn(Binding.builder().serviceBindingId(bindingId)
                        .resourceType(Route)
                        .resourceId(routeId).build());
        return new DeleteServiceInstanceBindingRequest(serviceId, bindingId, SERVICE_DEFINITION_ID, PLAN_ID, null);

    }

    @Test
    public void should_remove_route_on_deletion() throws Exception {
        String testId = "testDelRouteBinding";

        final DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppRouteTest(testId, testId, testId);

        //given that we known the route
        when(bindingRepository.findOne(testId)).thenReturn(BeanGenerator.createRouteBinding(testId));
        //when unbinding the route
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then it should be cleared from database
        verify(bindingRepository, times(1)).delete(testId);
    }

}