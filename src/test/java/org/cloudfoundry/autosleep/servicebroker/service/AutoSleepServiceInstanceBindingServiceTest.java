package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.dao.repositories.ram.RamServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.autosleep.servicebroker.configuration.AutosleepCatalogBuilder;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {AutosleepCatalogBuilder.class,
        RepositoryConfig.class})
public class AutoSleepServiceInstanceBindingServiceTest {

    private static final UUID APP_UID = UUID.randomUUID();


    private AutoSleepServiceInstanceBindingService bindingService;

    @Autowired
    private Catalog catalog;

    @Autowired
    private BindingRepository bindingRepo;

    @Autowired
    private ApplicationRepository appRepo;


    private CreateServiceInstanceBindingRequest createRequestTemplate;
    private GlobalWatcher mockWatcher;
    private String planId;
    private String serviceDefinitionId;

    /**
     * Init request templates with calaog definition, prepare mocks.
     */
    @Before
    public void init() {

        //mocking serviceRepo, we will just test bindingRepo in this class.
        ServiceRepository serviceRepo = mock(RamServiceRepository.class);
        when(serviceRepo.findOne(any(String.class))).thenReturn(mock(AutosleepServiceInstance.class));

        mockWatcher = mock(GlobalWatcher.class);

        bindingService = new AutoSleepServiceInstanceBindingService(appRepo,bindingRepo,mockWatcher);

        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        planId = serviceDefinition.getPlans().get(0).getId();
        serviceDefinitionId = serviceDefinition.getId();
        createRequestTemplate = new CreateServiceInstanceBindingRequest(serviceDefinitionId,
                planId,
                APP_UID.toString());
    }

    @Test
    public void testCreateServiceInstanceBinding() throws Exception {
        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId("Sid").withBindingId(
                "Bid"));
        verify(mockWatcher,times(1)).watchApp(any());
    }

    @Test
    public void testDeleteServiceInstanceBinding() throws Exception {
        String bindingId = "delBindingId";
        String serviceId = "delServiceId";

        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId(serviceId)
                .withBindingId(bindingId));
        DeleteServiceInstanceBindingRequest deleteRequest = new DeleteServiceInstanceBindingRequest(bindingId, null,
                serviceDefinitionId, planId);
        bindingService.deleteServiceInstanceBinding(deleteRequest);
    }

}