package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.repositories.ram.RamServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.configuration.CatalogConfiguration;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {CatalogConfiguration.class,
        AutoSleepServiceInstanceService.class,
        RamServiceRepository.class})
@ActiveProfiles("in-memory")
public class AutosleepServiceInstanceServiceTest {

    private static final String ORG_TEST = "orgTest";

    private static final String SPACE_TEST = "spaceTest";

    @Autowired
    private AutoSleepServiceInstanceService service;

    @Autowired
    private Catalog catalog;

    private CreateServiceInstanceRequest baseRequest;
    private UpdateServiceInstanceRequest updateRequest;
    private DeleteServiceInstanceRequest deleteRequest;

    /**Init a create request, to be used in tests.
     *
     */
    @Before
    public void initService() {
        assertTrue("Catalog must a least contain a catalog definition", catalog.getServiceDefinitions().size() > 0);
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertTrue("Service definition " + serviceDefinition.getId() + " must at least contain a plan",
                serviceDefinition.getPlans().size() > 0);
        baseRequest = new CreateServiceInstanceRequest(serviceDefinition.getId(), serviceDefinition
                .getPlans().get(0).getId(), ORG_TEST, SPACE_TEST);
        assertNotNull(baseRequest);

        updateRequest = new UpdateServiceInstanceRequest(serviceDefinition.getId());

        deleteRequest = new DeleteServiceInstanceRequest(serviceDefinition.getId(),
                "tobeupdated",
                serviceDefinition.getPlans().get(0).getId());
    }


    @Test
    public void testCreateServiceInstance() throws Exception {
        baseRequest.withServiceInstanceId("CreationTests");
        try {
            service.createServiceInstance(null);
            log.debug("Service instance created");
            fail("Succeed in creating service with no request");
        } catch (NullPointerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("inactivity","10H");
        try {
            baseRequest.setParameters(params);
            service.createServiceInstance(baseRequest);

            fail("Succeed in creating service with a request with wrong parameters");
        } catch (HttpMessageNotReadableException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        ServiceInstance si;

        params.put("inactivity","PT10H");
        try {
            baseRequest.setParameters(params);
            si = service.createServiceInstance( baseRequest );

            assertTrue("Succeed in creating service with inactivity parameter", si != null );
        } catch (ServiceBrokerException s) {
            fail("Fail to create service with inactivity parameter");
        }

        try {
            service.createServiceInstance( baseRequest );
            fail("Succeed in creating an already existing service");
        } catch (ServiceInstanceExistsException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }

        try {
            baseRequest.setParameters(null);
            si = service.createServiceInstance( baseRequest.withServiceInstanceId("other") );
            assertTrue("We should be able to create a service without parameters", si != null );
        } catch (RuntimeException e) {
            fail("Fail to create service with no parameters");
        }
    }

    @Test
    public void testGetServiceInstance() throws Exception {
        String testId = "testget";
        ServiceInstance createdInstance = service.createServiceInstance(baseRequest.withServiceInstanceId(testId));
        ServiceInstance retrievedInstance = service.getServiceInstance(testId);
        assertThat("Created instance and retrieved instance should be the same",createdInstance,
                is(equalTo(retrievedInstance)));
    }

    @Test
    public void testUpdateServiceInstance() throws Exception {
        String testId = "testupdate";
        service.createServiceInstance(baseRequest.withServiceInstanceId(testId));
        try  {
            service.updateServiceInstance(updateRequest.withInstanceId(testId));
            fail("update not supposed to work for now");
        } catch (ServiceInstanceUpdateNotSupportedException e)  {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }

        try  {
            service.updateServiceInstance(updateRequest.withInstanceId("unkownId"));
            fail("update not supposed to work on an unknown service id");
        } catch (ServiceInstanceDoesNotExistException e)  {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }
    }

    @Test
    public void testDeleteServiceInstance() throws Exception {
        service.createServiceInstance(baseRequest.withServiceInstanceId(deleteRequest.getServiceInstanceId()));
        ServiceInstance si = service.getServiceInstance(deleteRequest.getServiceInstanceId());
        assertTrue("Succeed in getting service ", si != null );
        si = service.deleteServiceInstance(deleteRequest);
        assertTrue("Succeed in deleting service ", si != null );

    }
}