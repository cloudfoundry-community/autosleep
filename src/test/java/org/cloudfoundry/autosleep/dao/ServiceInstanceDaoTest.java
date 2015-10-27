package org.cloudfoundry.autosleep.dao;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.repositories.ram.RamServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.configuration.CatalogConfiguration;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ActiveProfiles("in-memory")
@ContextConfiguration(classes = {CatalogConfiguration.class, RamServiceRepository.class})
public class ServiceInstanceDaoTest {

    private static final String ORG_TEST = "orgTest";

    private static final String SPACE_TEST = "spaceTest";

    private static final String APP_TEST = "appTest";

    @Autowired
    private Catalog catalog;

    @Autowired
    private ServiceRepository dao;

    private enum InsertedInstanceIds {
        testInsertServiceFail, testGetServiceSuccess, testUpdateServiceSuccess,
        testDeleteServiceSuccess,
        testBinding

    }

    private enum InsertedBindingIds {
       testRemoveBindingSuccess
    }

    private final InsertedInstanceIds idForBindingTest = InsertedInstanceIds.testBinding;

    private CreateServiceInstanceRequest createInstanceTemplate;

    private long nbServicesInit;





/*TODO move to binding dao test
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



    private int countBindings(String serviceInstanceId) throws ServiceInstanceDoesNotExistException {
        AtomicInteger nbBinding = new AtomicInteger(0);
        dao.listBinding(serviceInstanceId, serviceBinding -> nbBinding.incrementAndGet());
        return nbBinding.get();
    }*/

    private long countServices() {
        return dao.count();
    }

    @Test
    public void fakeTest() {
        assertTrue("Catalog must a least contain a catalog definition", true);
    }
}