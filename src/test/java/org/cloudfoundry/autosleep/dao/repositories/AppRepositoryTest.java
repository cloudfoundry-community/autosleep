package org.cloudfoundry.autosleep.dao.repositories;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.RepositoryConfig;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.toIntExact;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RepositoryConfig.class})
public abstract class AppRepositoryTest {

    private final Instant yesterday = Instant.now().minus(Duration.ofDays(1));
    private final Instant now = Instant.now();

    @Autowired
    private ApplicationRepository dao;

    /**
     * Clean db.
     */
    @Before
    @After
    public void clearDao() {
        dao.deleteAll();
    }

    private ApplicationInfo buildAppInfo(UUID uuid) {
        return  new ApplicationInfo(new ApplicationActivity(uuid, "appname", AppState.STARTED,
                Instant.now(), Instant.now()));
    }

    @Test
    public void testInsert() {
        dao.save(buildAppInfo(UUID.randomUUID()));
        assertThat(countTotal(), is(equalTo(1)));
    }

    @Test
    public void testMultipleInsertsAndRetrieves() {
        List<String> ids = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());


        List<ApplicationInfo> initialList = new ArrayList<>();
        ids.forEach(id -> initialList.add(buildAppInfo(UUID.fromString(id))));

        //test save all
        dao.save(initialList);
        assertThat("Count should be equal to the amount inserted", countTotal(), is(equalTo(initialList.size())));

        //test "exist"
        ids.forEach(id -> assertThat("Each element should exist in DAO", dao.exists(id), is(true)));

        //test that retrieving all elements give the same amount
        Iterable<ApplicationInfo> storedElement = dao.findAll();
        int count = 0;
        for (ApplicationInfo object : storedElement) {
            count++;
        }
        assertTrue("Retrieving all elements should return the same quantity", count == initialList.size());

        //test find with all inserted ids
        storedElement = dao.findAll(ids);
        for (ApplicationInfo object : storedElement) {
            assertTrue("Retrieved element should be the same as initial element", initialList.contains(object));
        }

    }

    @Test
    public void testFind() {
        UUID appId = UUID.randomUUID();
        ApplicationInfo original = buildAppInfo(appId);
        dao.save(original);
        ApplicationInfo retrieved = dao.findOne(appId.toString());
        assertFalse("Service binding should have been found", retrieved == null);
        assertThat(retrieved.getUuid(), is(equalTo(appId)));
        assertThat(retrieved, is(equalTo(original)));
        assertTrue("Succeed in getting a binding that does not exist", dao.findOne("thisAppShouldNotExist") == null);
    }



    @Test
    public void testCount() {
        assertThat(countTotal(), is(equalTo(0)));
    }


    @Test
    public void testDelete() {
        UUID deleteByIdSuccess = UUID.randomUUID();
        UUID deleteByInstanceSuccess = UUID.randomUUID();
        UUID deleteByMass1 = UUID.randomUUID();
        UUID deleteByMass2 = UUID.randomUUID();
        UUID deleteOtherRandom = UUID.randomUUID();

        dao.save(buildAppInfo(deleteByIdSuccess));
        dao.save(buildAppInfo(deleteByInstanceSuccess));
        dao.save(buildAppInfo(deleteByMass1));
        dao.save(buildAppInfo(deleteByMass2));
        dao.save(buildAppInfo(deleteOtherRandom));

        int nbServicesInit = 5;
        assertThat(countTotal(), is(equalTo(nbServicesInit)));

        //wrong id shouldn't raise anything
        dao.delete("testDeleteServiceFail");

        //delete a service by binding id
        dao.delete(deleteByIdSuccess.toString());
        assertThat(countTotal(), is(equalTo(nbServicesInit - 1)));

        //delete a service by name
        dao.delete(dao.findOne(deleteByInstanceSuccess.toString()));
        assertThat(countTotal(), is(equalTo(nbServicesInit - 2)));

        //delete multiple services
        Iterable<ApplicationInfo> apps = dao.findAll(Arrays.asList(deleteByMass1.toString(),
                deleteByMass2.toString()));
        dao.delete(apps);
        assertThat(countTotal(), is(equalTo(nbServicesInit - 4)));

        //delete all services
        dao.deleteAll();
        assertTrue(countTotal() == 0);

    }

    private int countTotal() {
        return toIntExact(dao.count());
    }
}