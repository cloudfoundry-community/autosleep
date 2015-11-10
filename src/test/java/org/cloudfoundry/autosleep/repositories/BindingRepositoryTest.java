package org.cloudfoundry.autosleep.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.RepositoryConfig;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
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
import static org.junit.Assert.*;


@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RepositoryConfig.class})
public abstract class BindingRepositoryTest {

    private static final String APP_GUID = "2F5A0947-6468-401B-B12A-963405121937";

    @Autowired
    private BindingRepository dao;

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
        dao.save(new AutoSleepServiceBinding("testInsert", "testInsert", null, null, APP_GUID));
        assertThat(countServices(), is(equalTo(1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() {
        List<String> ids = Arrays.asList("testInsert1", "testInsert2");
        String serviceId = "testServiceId";
        List<AutoSleepServiceBinding> initialList = new ArrayList<>();
        ids.forEach(id -> initialList.add(new AutoSleepServiceBinding(id, serviceId, null, null, APP_GUID)));

        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the amount inserted", countServices(), is(equalTo(
                initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<AutoSleepServiceBinding> storedElement = dao.findAll();
        int count = 0;
        for (AutoSleepServiceBinding object : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == initialList
                .size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        for (AutoSleepServiceBinding object : storedElement) {
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }

    }

    @Test
    public void testEquality() {
        String bindingId = "bidingIdEquality";
        String serviceId = "serviceIdEquality";
        AutoSleepServiceBinding original = new AutoSleepServiceBinding(bindingId, serviceId, null, null, APP_GUID);

        dao.save(original);
        AutoSleepServiceBinding binding = dao.findOne(bindingId);
        assertFalse("Service binding should have been found", binding == null);
        assertThat(binding.getServiceInstanceId(), is(equalTo(serviceId)));
        assertThat(binding.getAppGuid(), is(equalTo(APP_GUID)));
        assertThat(binding, is(equalTo(original)));
        assertTrue("Succeed in getting a binding that does not exist", dao.findOne("testGetServiceFail") == null);

    }



    @Test
    public void testCount() {
        assertThat(countServices(), is(equalTo(0)));
    }


    @Test
    public void testDelete() {
        String deleteByIdSuccess = "deleteByIdSuccess";
        String deleteByInstanceSuccess = "deleteByInstanceSuccess";
        String deleteByMass1 = "deleteByMass1";
        String deleteByMass2 = "deleteByMass2";
        dao.save(new AutoSleepServiceBinding(deleteByIdSuccess, "service", null, null, APP_GUID));
        dao.save(new AutoSleepServiceBinding(deleteByInstanceSuccess, "service", null, null, APP_GUID));
        dao.save(new AutoSleepServiceBinding(deleteByMass1, "service", null, null, APP_GUID));
        dao.save(new AutoSleepServiceBinding(deleteByMass2, "service", null, null, APP_GUID));

        int nbServicesInit = 4;
        assertThat(countServices(), is(equalTo(nbServicesInit)));

        //wrong id shouldn't raise anything
        dao.delete("testDeleteServiceFail");

        //delete a service by binding id
        dao.delete(deleteByIdSuccess);
        assertThat(countServices(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(deleteByInstanceSuccess));
        assertThat(countServices(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<AutoSleepServiceBinding> services = dao.findAll(Arrays.asList(deleteByMass1, deleteByMass2));
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