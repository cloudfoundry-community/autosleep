package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.configuration.AutosleepCatalogBuilder;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {AutosleepCatalogBuilder.class,
        AutoSleepServiceInstanceService.class,
        RepositoryConfig.class})
public class AutosleepServiceInstanceServiceTest {

    private static final String ORG_TEST = "orgTest";

    private static final String SPACE_TEST = "spaceTest";

    @Autowired
    private AutoSleepServiceInstanceService service;

    @Autowired
    private Catalog catalog;

    @Autowired
    private ServiceRepository serviceRepository;

    private CreateServiceInstanceRequest createRequest;
    private UpdateServiceInstanceRequest updateRequest;
    private DeleteServiceInstanceRequest deleteRequest;

    /**
     * Init a create request, to be used in tests.
     */
    @Before
    public void initService() {
        serviceRepository.deleteAll();

        assertTrue("Catalog must a least contain a catalog definition", catalog.getServiceDefinitions().size() > 0);
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertTrue("Service definition " + serviceDefinition.getId() + " must at least contain a plan",
                serviceDefinition.getPlans().size() > 0);
        createRequest = new CreateServiceInstanceRequest(serviceDefinition.getId(), serviceDefinition
                .getPlans().get(0).getId(), ORG_TEST, SPACE_TEST);

        updateRequest = new UpdateServiceInstanceRequest(serviceDefinition.getId());

        deleteRequest = new DeleteServiceInstanceRequest(serviceDefinition.getId(),
                "tobeupdated",
                serviceDefinition.getPlans().get(0).getId());
    }


    @Test
    public void testCreateServiceInstance() throws Exception {
        createRequest.withServiceInstanceId("CreationTests");
        try {
            service.createServiceInstance(null);
            log.debug("Service instance created");
            fail("Succeed in creating service with no request");
        } catch (NullPointerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        Map<String, Object> params = new HashMap<>();
        params.put(AutosleepServiceInstance.INACTIVITY_PARAMETER, "10H");
        try {
            createRequest.setParameters(params);
            service.createServiceInstance(createRequest);

            fail("Succeed in creating service with a request with wrong parameters");
        } catch (HttpMessageNotReadableException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        org.cloudfoundry.community.servicebroker.model.ServiceInstance si;

        params.put(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT10H");
        try {
            createRequest.setParameters(params);
            si = service.createServiceInstance(createRequest);

            assertTrue("Succeed in creating service with inactivity parameter", si != null);
        } catch (ServiceBrokerException s) {
            fail("Fail to create service with inactivity parameter");
        }

        try {
            service.createServiceInstance(createRequest);
            fail("Succeed in creating an already existing service");
        } catch (ServiceInstanceExistsException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }

        try {
            createRequest.setParameters(null);
            si = service.createServiceInstance(createRequest.withServiceInstanceId("other"));
            assertTrue("We should be able to create a service without parameters", si != null);
        } catch (RuntimeException e) {
            fail("Fail to create service with no parameters");
        }
    }

    @Test
    public void testGetServiceInstance() throws Exception {
        String testId = "testget";
        org.cloudfoundry.community.servicebroker.model.ServiceInstance createdInstance = service
                .createServiceInstance(createRequest.withServiceInstanceId(testId));
        org.cloudfoundry.community.servicebroker.model.ServiceInstance retrievedInstance = service.getServiceInstance(
                testId);
        assertThat("Created instance and retrieved instance should be the same", createdInstance,
                is(equalTo(retrievedInstance)));
    }

    @Test
    public void testUpdateServiceInstance() throws Exception {
        String testId = "testupdate";
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT10H"));
        service.createServiceInstance(createRequest.withServiceInstanceId(testId));
        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(testId);
        assertThat(serviceInstance, is(notNullValue()));
        assertThat(serviceInstance.getInterval(), is(equalTo(Duration.ofHours(10))));

        updateRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT15M"));
        service.updateServiceInstance(updateRequest.withInstanceId(testId));
        serviceInstance = serviceRepository.findOne(testId);
        assertThat(serviceInstance, is(notNullValue()));
        assertThat(serviceInstance.getInterval(), is(equalTo(Duration.ofMinutes(15))));

        try {
            service.updateServiceInstance(updateRequest.withInstanceId("unknownId"));
            fail("update not supposed to work on an unknown service id");
        } catch (ServiceInstanceDoesNotExistException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }
    }

    @Test
    public void testDeleteServiceInstance() throws Exception {
        service.createServiceInstance(createRequest.withServiceInstanceId(deleteRequest.getServiceInstanceId()));
        org.cloudfoundry.community.servicebroker.model.ServiceInstance si = service.getServiceInstance(deleteRequest
                .getServiceInstanceId());
        assertTrue("Succeed in getting service ", si != null);
        si = service.deleteServiceInstance(deleteRequest);
        assertTrue("Succeed in deleting service ", si != null);

    }

    @After
    public void clearDao() {
        serviceRepository.deleteAll();
    }

}