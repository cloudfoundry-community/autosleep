package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApi;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.repositories.ram.RamBindingRepository;
import org.cloudfoundry.autosleep.repositories.ram.RamServiceRepository;
import org.cloudfoundry.autosleep.scheduling.Clock;
import org.cloudfoundry.autosleep.servicebroker.configuration.CatalogConfiguration;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {CatalogConfiguration.class,
        RamBindingRepository.class})
@ActiveProfiles("in-memory")
public class AutoSleepServiceInstanceBindingServiceTest {

    private static final String APPUID = "DB1F7D54-7A6A-4F7C-A06E-43EF9B9E3144";


    private AutoSleepServiceInstanceBindingService bindingService;

    @Autowired
    private Catalog catalog;

    @Autowired
    private BindingRepository bindingRepo;

    private CreateServiceInstanceBindingRequest createRequestTemplate;

    private String planId;
    private String serviceDefinitionId;

    /**
     * Init request templates with calaog definition, prepare mocks.
     */
    @Before
    public void init() {

        Clock clock = mock(Clock.class);
        CloudFoundryApiService remote = mock(CloudFoundryApi.class);
        //mocking serviceRepo, we will just test bindingRepo in this class.
        ServiceRepository serviceRepo = mock(RamServiceRepository.class);
        when(serviceRepo.findOne(any(String.class))).thenReturn(mock(AutoSleepServiceInstance.class));

        bindingService = new AutoSleepServiceInstanceBindingService(clock, remote, serviceRepo, bindingRepo);

        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        planId = serviceDefinition.getPlans().get(0).getId();
        serviceDefinitionId = serviceDefinition.getId();
        createRequestTemplate = new CreateServiceInstanceBindingRequest(serviceDefinitionId,
                planId,
                APPUID);
    }

    @Test //TODO complete tests
    public void testCreateServiceInstanceBinding() throws Exception {
        bindingService.createServiceInstanceBinding(createRequestTemplate.withServiceInstanceId("Sid").withBindingId(
                "Bid"));
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