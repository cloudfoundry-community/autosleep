package org.cloudfoundry.autosleep.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.repositories.ram.RamServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("in-memory")
@ContextConfiguration(classes = {RamServiceRepository.class})
public class ServiceRepositoryTest {

    private static final String ORG_TEST = "orgTest";
    private static final String SPACE_TEST = "spaceTest";
    private static final String SERVICE_DEFINITION_ID = "TESTS";
    private static final String SERVICE_PLAN_ID = "PLAN";

    @Autowired
    private ServiceRepository dao;

    private enum InsertedInstanceIds {
        testInsertServiceFail, testGetServiceSuccess, testUpdateServiceSuccess,
        testDeleteServiceByIdSuccess,
        testDeleteServiceByInstanceSuccess,
        testDeleteMass01,
        testDeleteMass02,
    }

    private CreateServiceInstanceRequest createRequestTemplate;

    private long nbServicesInit;


    /**
     * Init DAO with test data.
     */
    @Before
    public void populateDao() {
        createRequestTemplate = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID, SERVICE_PLAN_ID, ORG_TEST,
                SPACE_TEST);

        dao.deleteAll();

        Arrays.asList(InsertedInstanceIds.values()).forEach(serviceInstanceId -> dao.save(new
                AutoSleepServiceInstance(createRequestTemplate.withServiceInstanceId(serviceInstanceId
                .name()))));
        nbServicesInit = countServices();

    }

    @Test
    public void testInsert() {
        dao.save(new AutoSleepServiceInstance(createRequestTemplate.withServiceInstanceId("testInsertServiceSuccess")));
        assertThat(countServices(), is(equalTo(nbServicesInit + 1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() throws ServiceInstanceExistsException, ServiceBrokerException {
        List<String> ids = Arrays.asList("testInsertServiceSuccess1", "testInsertServiceSuccess2");
        List<AutoSleepServiceInstance> initialList = new ArrayList<>();
        ids.forEach(id -> initialList.add(new AutoSleepServiceInstance(createRequestTemplate.withServiceInstanceId(
                id))));

        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the initial amount plus inserted", countServices(), is(equalTo(
                nbServicesInit + initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<AutoSleepServiceInstance> storedElement = dao.findAll();
        int count = 0;
        for (AutoSleepServiceInstance ignored : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == nbServicesInit + initialList
                .size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        for (AutoSleepServiceInstance object : storedElement) {
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }

    }

    @Test
    public void testGetService() {
        AutoSleepServiceInstance serviceInstance = dao.findOne(InsertedInstanceIds.testGetServiceSuccess.name());
        assertFalse("Service should have been found", serviceInstance == null);
        assertThat(serviceInstance.getServiceInstanceId(), is(
                equalTo(InsertedInstanceIds.testGetServiceSuccess.name())));
        assertThat(serviceInstance.getPlanId(), is(equalTo(createRequestTemplate.getPlanId())));
        assertThat(serviceInstance.getServiceDefinitionId(),
                is(equalTo(createRequestTemplate.getServiceDefinitionId())));
        assertThat(serviceInstance.getOrganizationGuid(), is(equalTo(ORG_TEST)));
        assertThat(serviceInstance.getSpaceGuid(), is(equalTo(SPACE_TEST)));
        assertThat(serviceInstance.getInterval(), is(equalTo(Config.defaultInactivityPeriod)));
        assertTrue("Succeed in getting a service that does not exist", dao.findOne("testGetServiceFail") == null);
    }

    @Test
    public void testListServices() {
        assertThat(countServices(), is(equalTo(nbServicesInit)));
    }


    @Test
    public void testDelete() {
        //wrong id shouldn't raise anything
        dao.delete("testDeleteServiceFail");

        //delete a service by id
        dao.delete(InsertedInstanceIds.testDeleteServiceByIdSuccess.name());
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(InsertedInstanceIds.testDeleteServiceByInstanceSuccess.name()));
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<AutoSleepServiceInstance> services = dao.findAll(Arrays.asList(InsertedInstanceIds.testDeleteMass01
                .name(), InsertedInstanceIds.testDeleteMass02.name()));
        dao.delete(services);
        assertThat(countServices(), is(equalTo(nbServicesInit - 4)));

        //delete all services
        dao.deleteAll();
        assertTrue(countServices() == 0);

    }


    private long countServices() {
        return dao.count();
    }
}