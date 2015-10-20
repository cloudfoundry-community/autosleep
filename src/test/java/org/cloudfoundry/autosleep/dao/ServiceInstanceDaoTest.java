package org.cloudfoundry.autosleep.dao;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.servicebroker.configuration.CatalogConfiguration;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {CatalogConfiguration.class, ServiceInstanceDao.class})
public class ServiceInstanceDaoTest {

    private static final String ORG_TEST = "orgTest";

    private static final String SPACE_TEST = "spaceTest";

    private static final String APP_TEST = "appTest";

    @Autowired
    private Catalog catalog;

    @Autowired
    private ServiceInstanceDaoService dao;

    private enum InsertedInstanceIds {
        testInsertServiceFail, testGetServiceSuccess, testUpdateServiceSuccess,
        testDeleteServiceSuccess,
        testBinding

    }

    private enum InsertedBindingIds {
        testAddBindingFail, testListBinding, testRemoveBindingSuccess
    }

    private final InsertedInstanceIds idForBindingTest = InsertedInstanceIds.testBinding;

    private CreateServiceInstanceRequest createInstanceTemplate;

    private int nbServicesInit;

    private int nbBindingInserted;


    /** Init DAO with test data
     *
     * @throws ServiceInstanceDoesNotExistException
     */
    @Before
    public void populateDao() throws ServiceInstanceDoesNotExistException {
        assertTrue("Catalog must a least contain a catalog definition", catalog.getServiceDefinitions().size() > 0);
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertTrue("Service definition " + serviceDefinition.getId() + " must at least contain a plan",
                serviceDefinition.getPlans().size() > 0);
        createInstanceTemplate = new CreateServiceInstanceRequest(serviceDefinition.getId(), serviceDefinition
                .getPlans().get(0).getId(), ORG_TEST, SPACE_TEST);


        dao.purge();

        Arrays.asList(InsertedInstanceIds.values()).forEach(serviceInstanceId -> {
            try {
                dao.insertService(new ServiceInstance(createInstanceTemplate.withServiceInstanceId(serviceInstanceId
                        .name())), Duration.ofSeconds(1));
            } catch (ServiceInstanceExistsException s) {
                log.error("error while inserting " + serviceInstanceId + ". It already exists");
            }
        });
        Arrays.asList(InsertedBindingIds.values()).forEach(serviceBindingId -> {
            try {
                dao.addBinding(idForBindingTest.name(), new ServiceInstanceBinding(serviceBindingId
                        .name(),
                        idForBindingTest.name(), null, null, APP_TEST));
            } catch (ServiceInstanceBindingExistsException s) {
                log.error("error while adding binding " + serviceBindingId + ". It already exists");
            }
        });
        nbServicesInit = countServices();
        nbBindingInserted = countBindings(idForBindingTest.name());
    }

    @Test
    public void testInsertService() throws ServiceInstanceExistsException {
        dao.insertService(new ServiceInstance(
                createInstanceTemplate.withServiceInstanceId("testInsertServiceSuccess")), Duration.ofSeconds(1));
        assertThat(countServices(), is(equalTo(nbServicesInit + 1)));
        try {
            dao.insertService(new ServiceInstance(
                    createInstanceTemplate.withServiceInstanceId("testInsertServiceFail")), Duration.ofSeconds(1));
            fail("Succeed in inserting a service that already existed");
        } catch (ServiceInstanceExistsException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }
    }

    @Test
    public void testGetService() {
        ServiceInstance serviceInstance = dao.getService(InsertedInstanceIds.testGetServiceSuccess.name());
        assertFalse("Service should have been found", serviceInstance == null);
        assertThat(serviceInstance.getServiceInstanceId(), is(
                equalTo(InsertedInstanceIds.testGetServiceSuccess.name())));
        assertThat(serviceInstance.getPlanId(), is(equalTo(createInstanceTemplate.getPlanId())));
        assertThat(serviceInstance.getServiceDefinitionId(),
                is(equalTo(createInstanceTemplate.getServiceDefinitionId())));
        assertThat(serviceInstance.getOrganizationGuid(), is(equalTo(ORG_TEST)));
        assertThat(serviceInstance.getSpaceGuid(), is(equalTo(SPACE_TEST)));
        assertTrue("Succeed in getting a service that does not exist", dao.getService("testGetServiceFail") == null);
    }

    @Test
    public void testListServices() {
        assertThat(countServices(), is(equalTo(nbServicesInit)));
    }

    @Test
    public void testUpdateService() throws ServiceInstanceDoesNotExistException {
        try {
            UpdateServiceInstanceRequest updateRequest = new UpdateServiceInstanceRequest(createInstanceTemplate
                    .getPlanId()
                    + "-next");
            dao.updateService(new ServiceInstance(updateRequest.withInstanceId(InsertedInstanceIds
                    .testUpdateServiceSuccess.name())), Duration.ofSeconds(1));
            fail("update should not be supported");
        } catch (ServiceInstanceUpdateNotSupportedException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }
    }

    @Test
    public void testDeleteService() {
        assertFalse("Service should have been found and deleted", dao.deleteService(InsertedInstanceIds
                .testDeleteServiceSuccess.name()) == null);
        assertTrue("Service should not have been found", dao.deleteService("testDeleteServiceFail") == null);
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));
    }

    @Test
    public void testAddBinding() throws ServiceInstanceBindingExistsException, ServiceInstanceDoesNotExistException {
        dao.addBinding(idForBindingTest.name(), new ServiceInstanceBinding("testAddBindingSuccess",
                idForBindingTest.name(), null, null, APP_TEST));
        assertThat(countBindings(idForBindingTest.name()), is(equalTo(nbBindingInserted + 1)));
    }

    @Test
    public void testListBinding() throws ServiceInstanceDoesNotExistException {
        assertThat(countBindings(idForBindingTest.name()), is(equalTo(nbBindingInserted)));
    }

    @Test
    public void testRemoveBinding() throws ServiceInstanceDoesNotExistException {
        assertFalse("Service binding shoould have been removed", dao.removeBinding(idForBindingTest.name(),
                InsertedBindingIds.testRemoveBindingSuccess.name()) == null);
        assertThat(countBindings(idForBindingTest.name()), is(equalTo(nbBindingInserted - 1)));
        assertTrue("Service binding should not have been found", dao.removeBinding(idForBindingTest.name(),
                "testRemoveBindingFailure") == null);
    }

    private int countServices() {
        AtomicInteger servicesRead = new AtomicInteger(0);
        dao.listServices(serviceInstance -> servicesRead.incrementAndGet());
        return servicesRead.get();
    }


    private int countBindings(String serviceInstanceId) throws ServiceInstanceDoesNotExistException {
        AtomicInteger nbBinding = new AtomicInteger(0);
        dao.listBinding(serviceInstanceId, serviceBinding -> nbBinding.incrementAndGet());
        return nbBinding.get();
    }
}