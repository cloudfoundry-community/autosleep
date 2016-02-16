package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config.RouteBindingParameters;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo.EnrollmentState.State;
import org.cloudfoundry.autosleep.dao.model.RouteBinding;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.RouteBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceBindingResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
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
    private ApplicationBindingRepository appBindingRepo;

    @Mock
    private ApplicationRepository appRepo;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private ApplicationLocker applicationLocker;

    @InjectMocks
    private AutosleepBindingService bindingService;

    private CreateServiceInstanceBindingRequest createAppBindingTemplate;

    private CreateServiceInstanceBindingRequest createRouteBindingTemplate;

    @Mock
    private ApplicationInfo.EnrollmentState enrollmentState;

    @Mock
    private RouteBindingRepository routeBindingRepo;

    @Mock
    private SpaceEnrollerConfig spaceEnrollerConfig;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Mock
    private WorkerManagerService workerManager;

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

        HashMap<String, Object> bindingParams = new HashMap<>();
        bindingParams.put(RouteBindingParameters.linkedApplicationId, APP_UID);
        bindingParams.put(RouteBindingParameters.linkedApplicationBindingId, APP_UID + "bid");

        createRouteBindingTemplate = new CreateServiceInstanceBindingRequest(SERVICE_DEFINITION_ID,
                PLAN_ID,
                null,
                Collections.singletonMap(ServiceBindingResource.BIND_RESOURCE_KEY_ROUTE.toString(), ROUTE_UID),
                bindingParams);

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
        verify(appBindingRepo, times(1)).save(any(ApplicationBinding.class));
        verify(workerManager, times(1)).registerApplicationStopper(any(SpaceEnrollerConfig.class), anyString());
    }

    /**
     * Test that only autosleep can call itself on a route binding (no manual route binding).
     */
    @Test
    public void new_route_binding_should_scream_if_unknown_appid() {
        //given that the application is unknown
        when(appRepo.findOne(APP_UID)).thenReturn(null);

        //when receive a new ROUTE binding
        //should raise an exception
        verifyThrown(() -> bindingService.createServiceInstanceBinding(createRouteBindingTemplate
                .withServiceInstanceId("Sid").withBindingId("bidshouldfail")), ServiceBrokerException.class);
    }

    @Test
    public void new_route_binding_should_store_binding_and_create_a_proxy_route() {
        //given that the application is known
        when(appRepo.findOne(APP_UID)).thenReturn(applicationInfo);

        //when receive a new ROUTE binding
        bindingService.createServiceInstanceBinding(createRouteBindingTemplate.withServiceInstanceId("Sid")
                .withBindingId("Bidroute"));

        //then create it and store it to repository
        verify(appRepo, never()).save(any(ApplicationInfo.class));
        verify(routeBindingRepo, times(1)).save(any(RouteBinding.class));
        //TODO add check new route
    }

    private DeleteServiceInstanceBindingRequest prepareDeleteAppBindingTest(String serviceId, String bindingId) {

        when(appRepo.findOne(APP_UID)).thenReturn(applicationInfo);
        when(appBindingRepo.findOne(bindingId))
                .thenReturn(ApplicationBinding.builder().serviceBindingId(bindingId)
                        .serviceInstanceId(serviceId)
                        .applicationId(APP_UID).build());
        return new DeleteServiceInstanceBindingRequest(serviceId, bindingId, SERVICE_DEFINITION_ID, PLAN_ID, null);

    }

    private DeleteServiceInstanceBindingRequest prepareDeleteAppRouteTest(String serviceId, String bindingId, String
            routeId) {

        when(appRepo.findOne(APP_UID)).thenReturn(applicationInfo);
        when(routeBindingRepo.findOne(bindingId))
                .thenReturn(RouteBinding.builder().bindingId(bindingId).routeId(routeId)
                        .configurationId(serviceId)
                        .localRoute("localRoute")
                        .linkedApplicationId(APP_UID)
                        .linkedApplicationBindingId(bindingId + APP_UID).build());
        return new DeleteServiceInstanceBindingRequest(serviceId, bindingId, SERVICE_DEFINITION_ID, PLAN_ID, null);

    }

    @Test
    public void should_also_remove_route_binding_when_removing_app_binding() throws Exception {
        String testId = "testCascadeBindingDeletion";
        String linkedRouteBindingId = testId + "linkedRouteBinding";

        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppBindingTest(testId, testId);

        //given that a route binding is also registered
        when(routeBindingRepo.findAll()).thenReturn(Collections.singletonList(
                BeanGenerator.createRouteBinding(linkedRouteBindingId,testId,APP_UID,testId)));

        //when unbinding the app
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then app binding should be cleared from database, and CF API should be called to unbind route
        verify(appRepo, times(1)).delete(applicationInfo.getUuid());
        //TODO uncomment when ready verify(cfApi,times(1)).unbind(linkedRouteBindingId)

    }

    @Test
    public void should_blacklist_app_on_app_binding_deletion_if_autoenrollment_is_standard() throws Exception {
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
        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppBindingTest(serviceId, bindingId);
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then it should be blacklisted and kept in database
        verify(enrollmentState, times(1)).updateEnrollment(eq(serviceId), eq(true));
        verify(appRepo, times(1)).save(applicationInfo);
        //while binding should be deleted
        verify(appBindingRepo, times(1)).delete(bindingId);

    }

    @Test
    public void should_clear_app_on_app_binding_deletion_if_autoenrollment_is_forced() throws Exception {
        String bindingId = "testDelBindingForcedEnrollment";
        String serviceId = "testDelBindingForcedEnrollment";
        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppBindingTest(serviceId, bindingId);

        //given that autoEnrollment is forced
        when(spaceEnrollerConfig.isForcedAutoEnrollment()).thenReturn(true);

        //when unbinding the app
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then it should be cleared from database
        verify(enrollmentState, times(1)).updateEnrollment(anyString(), eq(false));
        verify(appRepo, times(1)).delete(applicationInfo.getUuid());
        //while binding should be deleted
        verify(appBindingRepo, times(1)).delete(bindingId);
    }

    @Test
    public void should_remove_route_on_deletion() throws Exception {
        String testId = "testDelRouteBinding";

        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteAppRouteTest(testId, testId, testId);

        //given that we known the route
        when(routeBindingRepo.findOne(testId)).thenReturn(BeanGenerator.createRouteBinding(testId));
        //when unbinding the route
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        //then it should be cleared from database
        verify(routeBindingRepo, times(1)).delete(testId);

    }

}