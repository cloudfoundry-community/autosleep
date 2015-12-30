package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ApplicationBindingServiceTest {

    private static final UUID APP_UID = UUID.randomUUID();

    private static final String SERVICE_DEFINITION_ID = "serviceDefinitionId";

    private static final String PLAN_ID = "planId";

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Mock
    private BindingRepository bindingRepo;

    @Mock
    private ApplicationRepository appRepo;

    @Mock
    private WorkerManagerService workerManager;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private ApplicationInfo.EnrollmentState enrollmentState;


    @Mock
    private ServiceInstance serviceInstance;

    @Mock
    private SpaceEnrollerConfig spaceEnrollerConfig;

    @Mock
    private ApplicationLocker applicationLocker;

    @InjectMocks
    private ApplicationBindingService bindingService;


    private CreateServiceInstanceBindingRequest createRequestTemplate;


    /**
     * Init request templates with calaog definition, prepare mocks.
     */
    @Before
    public void init() {
        createRequestTemplate = new CreateServiceInstanceBindingRequest(SERVICE_DEFINITION_ID,
                PLAN_ID,
                APP_UID.toString());
        when(applicationInfo.getUuid()).thenReturn(APP_UID.toString());
        when(applicationInfo.getEnrollmentState()).thenReturn(enrollmentState);
        when(spaceEnrollerConfigRepository.findOne(any(String.class))).thenReturn(spaceEnrollerConfig);

        //avoir nullpointer when getting credentials
        when(spaceEnrollerConfig.getIdleDuration()).thenReturn(Duration.ofSeconds(10));

        doAnswer(invocationOnMock -> {
            ((Runnable)invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));

    }

    @Test
    public void testCreateServiceInstanceBinding() throws Exception {
        when(appRepo.findOne(APP_UID.toString())).thenReturn(null);
        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId("Sid").withBindingId(
                "Bid"));
        verify(appRepo, times(1)).save(any(ApplicationInfo.class));
        verify(bindingRepo, times(1)).save(any(ApplicationBinding.class));
        verify(workerManager, times(1)).registerApplicationStopper(any(SpaceEnrollerConfig.class), anyString());

        when(appRepo.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId("Sid").withBindingId(
                "Bid"));
    }

    private DeleteServiceInstanceBindingRequest prepareDeleteTest(String serviceId , String bindingId) {

        when(appRepo.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        when(bindingRepo.findOne(bindingId))
                .thenReturn(ApplicationBinding.builder().serviceBindingId(bindingId)
                        .serviceInstanceId(serviceId)
                        .applicationId(APP_UID.toString()).build());

        return new DeleteServiceInstanceBindingRequest(bindingId,
                serviceInstance,
                SERVICE_DEFINITION_ID, PLAN_ID);
    }


    @Test
    public void testDeleteServiceInstanceBinding() throws Exception {
        final String bindingId = "testDelBinding";
        final String serviceId = "testDelBinding";

        when(spaceEnrollerConfig.isForcedAutoEnrollment()).thenReturn(false);

        HashMap<String,ApplicationInfo.EnrollmentState.State> services = new HashMap<>();
        services.put(serviceId, ApplicationInfo.EnrollmentState.State.BLACKLISTED);
        when(enrollmentState.getStates()).thenReturn(services);

        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteTest(serviceId,bindingId);
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        verify(enrollmentState,times(1)).updateEnrollment(anyString(), eq(true));

        verify(bindingRepo, times(1)).delete(bindingId);
        verify(appRepo, times(1)).save(applicationInfo);

    }

    @Test
    public void testDeleteServiceInstanceOnNoOptOut() throws Exception {
        String bindingId = "testDelBindingOnNoOptout";
        String serviceId = "testDelBindingOnNoOptout";

        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteTest(serviceId,bindingId);
        when(spaceEnrollerConfig.isForcedAutoEnrollment()).thenReturn(true);

        bindingService.deleteServiceInstanceBinding(deleteRequest);

        verify(enrollmentState,times(1)).updateEnrollment(anyString(), eq(false));

        verify(bindingRepo, times(1)).delete(bindingId);
        verify(appRepo, times(1)).delete(applicationInfo.getUuid().toString());
    }


}