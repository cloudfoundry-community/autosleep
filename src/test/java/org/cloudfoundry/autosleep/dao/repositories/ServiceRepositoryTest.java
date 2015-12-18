package org.cloudfoundry.autosleep.dao.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.util.ApplicationConfiguration;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class ServiceRepositoryTest {

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

    private Duration duration = Duration.ofMinutes(15);
    private Pattern excludePattern = Pattern.compile(".*");


    private long nbServicesInit;


    /**
     * Init DAO with test data.
     */
    @Before
    public void populateDao() {
        Duration duration = Duration.ofMinutes(15);
        Pattern excludePattern = Pattern.compile(".*");

        dao.deleteAll();

        Arrays.asList(InsertedInstanceIds.values()).forEach(serviceInstanceId -> dao.save(
                AutosleepServiceInstance.builder()
                        .serviceDefinitionId(SERVICE_DEFINITION_ID)
                        .planId(SERVICE_PLAN_ID)
                        .organizationId(ORG_TEST)
                        .spaceId(SPACE_TEST)
                        .serviceInstanceId(serviceInstanceId.name())
                        .idleDuration(duration)
                        .excludeFromAutoEnrollment(excludePattern)
                        .build()));
        nbServicesInit = countServices();

    }

    @Test
    public void testInsert() {
        dao.save(AutosleepServiceInstance.builder().serviceInstanceId("testInsertServiceSuccess").build());
        assertThat(countServices(), is(equalTo(nbServicesInit + 1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() throws ServiceInstanceExistsException, ServiceBrokerException {
        List<String> ids = Arrays.asList("testInsertServiceSuccess1", "testInsertServiceSuccess2");
        List<AutosleepServiceInstance> initialList = ids.stream()
                .map(id -> AutosleepServiceInstance.builder().serviceInstanceId(id).build())
                .collect(Collectors.toList());


        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the initial amount plus inserted", countServices(), is(equalTo(
                nbServicesInit + initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<AutosleepServiceInstance> storedElement = dao.findAll();
        int count = 0;
        for (AutosleepServiceInstance ignored : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == nbServicesInit + initialList
                .size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        log.debug("initial list {}", initialList.size());
        for (AutosleepServiceInstance object : initialList) {
            log.debug("initial object {} ", object.getServiceInstanceId());
        }
        for (AutosleepServiceInstance object : storedElement) {
            log.debug("found object {} , checking if in initial list", object.getServiceInstanceId());
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }

    }

    @Test
    public void testGetServiceEquality() {
        String serviceId = "GetTest";
        AutosleepServiceInstance originalService = AutosleepServiceInstance.builder()
                .serviceInstanceId(serviceId).build();
        dao.save(originalService);
        AutosleepServiceInstance serviceInstance = dao.findOne(serviceId);
        assertThat("Two objects should be equal", serviceInstance, is(equalTo(originalService)));
    }

    @Test
    public void testGetServiceByFields() {

        AutosleepServiceInstance serviceInstance = dao.findOne(InsertedInstanceIds.testGetServiceSuccess.name());
        assertFalse("Service should have been found", serviceInstance == null);
        assertThat(serviceInstance.getServiceInstanceId(), is(
                equalTo(InsertedInstanceIds.testGetServiceSuccess.name())));
        assertThat(serviceInstance.getPlanId(), is(equalTo(SERVICE_PLAN_ID)));
        assertThat(serviceInstance.getServiceDefinitionId(),
                is(equalTo(SERVICE_DEFINITION_ID)));
        assertThat(serviceInstance.getOrganizationId(), is(equalTo(ORG_TEST)));
        assertThat(serviceInstance.getSpaceId(), is(equalTo(SPACE_TEST)));
        assertThat(serviceInstance.getIdleDuration(), is(equalTo(duration)));
        assertThat(serviceInstance.getExcludeFromAutoEnrollment().pattern(), is(equalTo(excludePattern.pattern())));
        assertTrue("Succeed in getting a service that does not exist", dao.findOne("testGetServiceFail") == null);

    }

    @Test
    public void testListServices() {
        assertThat(countServices(), is(equalTo(nbServicesInit)));
    }


    @Test
    public void testDelete() {

        //delete a service by id
        dao.delete(InsertedInstanceIds.testDeleteServiceByIdSuccess.name());
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(InsertedInstanceIds.testDeleteServiceByInstanceSuccess.name()));
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<AutosleepServiceInstance> services = dao.findAll(Arrays.asList(InsertedInstanceIds.testDeleteMass01
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