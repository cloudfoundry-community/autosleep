package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.ApplicationStateMachine;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class AutoSleepServiceInstanceBindingServiceTest {

    private static final UUID APP_UID = UUID.randomUUID();

    private static final String SERVICE_DEFINITION_ID = "serviceDefinitionId";

    private static final String PLAN_ID = "planId";

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BindingRepository bindingRepo;

    @Mock
    private ApplicationRepository appRepo;

    @Mock
    private GlobalWatcher watcher;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private ApplicationStateMachine applicationStateMachine;

    @Mock
    private AutosleepServiceInstance serviceInstance;

    @InjectMocks
    private AutoSleepServiceInstanceBindingService bindingService;


    private CreateServiceInstanceBindingRequest createRequestTemplate;


    /**
     * Init request templates with calaog definition, prepare mocks.
     */
    @Before
    public void init() {
        createRequestTemplate = new CreateServiceInstanceBindingRequest(SERVICE_DEFINITION_ID,
                PLAN_ID,
                APP_UID.toString());
        when(applicationInfo.getUuid()).thenReturn(APP_UID);
        when(applicationInfo.getStateMachine()).thenReturn(applicationStateMachine);
        when(serviceRepository.findOne(any(String.class))).thenReturn(serviceInstance);
        //avoir nullpointer when getting credentials
        when(serviceInstance.getInterval()).thenReturn(Duration.ofSeconds(10));
    }

    @Test
    public void testCreateServiceInstanceBinding() throws Exception {
        when(appRepo.findOne(APP_UID.toString())).thenReturn(null);
        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId("Sid").withBindingId(
                "Bid"));
        verify(appRepo, times(1)).save(any(ApplicationInfo.class));
        verify(bindingRepo, times(1)).save(any(ApplicationBinding.class));
        verify(watcher, times(1)).watchApp(any());

        when(appRepo.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId("Sid").withBindingId(
                "Bid"));
        verify(applicationStateMachine, times(1)).onOptIn();
    }

    @Test
    public void testDeleteServiceInstanceBinding() throws Exception {
        String bindingId = "delBindingId";
        String serviceId = "delServiceId";


        when(appRepo.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        when(bindingRepo.findOne(bindingId))
                .thenReturn(new ApplicationBinding(bindingId, serviceId, null, null, APP_UID.toString()));

        DeleteServiceInstanceBindingRequest deleteRequest = new DeleteServiceInstanceBindingRequest(bindingId,
                serviceInstance,
                SERVICE_DEFINITION_ID, PLAN_ID);
        when(serviceInstance.isNoOptOut()).thenReturn(true);
        verifyThrown(() -> bindingService.deleteServiceInstanceBinding(deleteRequest), ServiceBrokerException.class,
                exceptionThrown -> verify(bindingRepo, never()).delete(bindingId));



        when(serviceInstance.isNoOptOut()).thenReturn(false);
        bindingService.deleteServiceInstanceBinding(deleteRequest);
        verify(bindingRepo, times(1)).delete(bindingId);
        verify(applicationStateMachine, times(1)).onOptOut();
        verify(appRepo, times(1)).save(applicationInfo);

    }

}