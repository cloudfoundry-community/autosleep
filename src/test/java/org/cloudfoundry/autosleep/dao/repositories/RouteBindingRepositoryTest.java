package org.cloudfoundry.autosleep.dao.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.RouteBinding;
import org.cloudfoundry.autosleep.util.ApplicationConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.toIntExact;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class, RepositoryConfig.class})
public abstract class RouteBindingRepositoryTest {

    private static final String ROUTE_UID = "2F5A0947-6468-401B-B12A-963405121937";

    @Autowired
    private RouteBindingRepository dao;

    /**
     * Init DAO with test data.
     */
    @Before
    @After
    public void clearDao() {
        dao.deleteAll();
    }

    @Test
    public void testInsert() {
        dao.save(RouteBinding.builder().configurationId("testInsert")
                .bindingId("testInsert").routeId(ROUTE_UID).build());
        assertThat(countServices(), is(equalTo(1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() {
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        List<RouteBinding> initialList = new ArrayList<>();
        ids.forEach(id -> initialList.add(RouteBinding.builder().bindingId(id)
                .configurationId(serviceId).routeId(ROUTE_UID).build()));

        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the amount inserted", countServices(), is(equalTo(
                initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<RouteBinding> storedElement = dao.findAll();
        int count = 0;
        for (RouteBinding object : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == initialList
                .size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        for (RouteBinding object : storedElement) {
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }

    }

    @Test
    public void testEquality() {
        String bindingId = "bidingIdEquality";
        String serviceId = "serviceIdEquality";
        RouteBinding original = RouteBinding.builder().bindingId(bindingId)
                .configurationId(serviceId).routeId(ROUTE_UID).build();

        dao.save(original);
        RouteBinding binding = dao.findOne(bindingId);
        assertFalse("Service binding should have been found", binding == null);
        assertThat(binding.getConfigurationId(), is(equalTo(serviceId)));
        assertThat(binding.getRouteId(), is(equalTo(ROUTE_UID)));
        assertThat(binding, is(equalTo(original)));
        assertTrue("Succeed in getting a binding that does not exist", dao.findOne("testGetServiceFail") == null);

    }


    @Test
    public void testCount() {
        assertThat(countServices(), is(equalTo(0)));
    }


    @Test
    public void testDelete() {
        final String deleteByIdSuccess = "deleteByIdSuccess";
        final String deleteByInstanceSuccess = "deleteByInstanceSuccess";
        final String deleteByMass1 = "deleteByMass1";
        final String deleteByMass2 = "deleteByMass2";
        RouteBinding.RouteBindingBuilder builder = RouteBinding.builder();
        builder.routeId(ROUTE_UID).configurationId("service");
        dao.save(builder.bindingId(deleteByIdSuccess).build());
        dao.save(builder.bindingId(deleteByInstanceSuccess).build());
        dao.save(builder.bindingId(deleteByMass1).build());
        dao.save(builder.bindingId(deleteByMass2).build());

        int nbServicesInit = 4;
        assertThat(countServices(), is(equalTo(nbServicesInit)));

        //delete a service by binding id
        dao.delete(deleteByIdSuccess);
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(deleteByInstanceSuccess));
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<RouteBinding> services = dao.findAll(Arrays.asList(deleteByMass1, deleteByMass2));
        dao.delete(services);
        assertThat(countServices(), is(equalTo(nbServicesInit - 4)));

        //delete all services
        dao.deleteAll();
        assertTrue(countServices() == 0);

    }

    private int countServices() {
        return toIntExact(dao.count());
    }
}