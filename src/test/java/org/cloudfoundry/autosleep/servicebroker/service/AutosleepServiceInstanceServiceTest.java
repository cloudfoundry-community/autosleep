package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.ServiceInstanceDao;
import org.cloudfoundry.autosleep.servicebroker.configuration.CatalogConfiguration;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {CatalogConfiguration.class,
        AutosleepServiceInstanceService.class,
        ServiceInstanceDao.class})
public class AutosleepServiceInstanceServiceTest {

    private static final String ORG_TEST = "orgTest";

    private static final String SPACE_TEST = "spaceTest";

    @Autowired
    private AutosleepServiceInstanceService service;

    @Autowired
    private Catalog catalog;

    private CreateServiceInstanceRequest baseRequest;

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
    }


    @Test
    public void testCreateServiceInstance() throws Exception {
        try {
            service.createServiceInstance(null);
            log.debug("Service instance created");
            fail("Succeed in creating service with no request");
        } catch (NullPointerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        try {
            log.debug("Creating service instance with request " + baseRequest.toString());
            service.createServiceInstance( baseRequest );
            log.debug("Service instance created");
            fail("Succeed in creating service with a request without parameters");
        } catch (ServiceBrokerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("inactivity","10H");
        try {
            baseRequest.setParameters(params);
            service.createServiceInstance( baseRequest );

            fail("Succeed in creating service with a request with wrong parameters");
        } catch (ServiceBrokerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }

        params.put("inactivity","PT10H");
        try {
            baseRequest.setParameters(params);
            ServiceInstance si = service.createServiceInstance( baseRequest );

            assertTrue("Succeed in creating service with inactivity parameter", si != null );
        } catch (ServiceBrokerException s) {
            fail("Fail to create service");
        }
    }

    @Test
    public void testGetServiceInstance() throws Exception {

    }

    @Test
    public void testUpdateServiceInstance() throws Exception {

    }

    @Test
    public void testDeleteServiceInstance() throws Exception {

    }
}