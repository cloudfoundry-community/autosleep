package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.Mockito.*;

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
    }

    private DeleteServiceInstanceBindingRequest prepareDeleteTest(String serviceId , String bindingId) {

        when(appRepo.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        when(bindingRepo.findOne(bindingId))
                .thenReturn(new ApplicationBinding(bindingId, serviceId, null, null, APP_UID.toString()));

        return new DeleteServiceInstanceBindingRequest(bindingId,
                serviceInstance,
                SERVICE_DEFINITION_ID, PLAN_ID);
    }


    @Test
    public void testDeleteServiceInstanceBinding() throws Exception {
        String bindingId = "testDelBinding";
        String serviceId = "testDelBinding";

        when(serviceInstance.isNoOptOut()).thenReturn(false);

        HashMap<String,ApplicationInfo.ServiceInstanceState> services = new HashMap<>();
        services.put(serviceId, ApplicationInfo.ServiceInstanceState.BLACKLISTED);
        when(applicationInfo.getServiceInstances()).thenReturn(services);

        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteTest(serviceId,bindingId);
        bindingService.deleteServiceInstanceBinding(deleteRequest);

        verify(applicationInfo,times(1)).removeBoundService(anyString(),eq(true));

        verify(bindingRepo, times(1)).delete(bindingId);
        verify(appRepo, times(1)).save(applicationInfo);

    }

    @Test
    public void testDeleteServiceInstanceOnNoOptOut() throws Exception {
        String bindingId = "testDelBindingOnNoOptout";
        String serviceId = "testDelBindingOnNoOptout";

        DeleteServiceInstanceBindingRequest deleteRequest = prepareDeleteTest(serviceId,bindingId);
        when(serviceInstance.isNoOptOut()).thenReturn(true);

        bindingService.deleteServiceInstanceBinding(deleteRequest);

        verify(applicationInfo,times(1)).removeBoundService(anyString(),eq(false));

        verify(bindingRepo, times(1)).delete(bindingId);
        verify(appRepo, times(1)).delete(applicationInfo.getUuid().toString());
    }


}