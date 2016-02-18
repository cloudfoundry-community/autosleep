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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private static final String CONFIGURATION_ID = "service";

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

    private int countServices() {
        return toIntExact(dao.count());
    }

    private void insertSomeIds(String... idsToInsert) {
        final RouteBinding.RouteBindingBuilder builder = RouteBinding.builder();
        builder.routeId(ROUTE_UID).configurationId(CONFIGURATION_ID);
        Arrays.asList(idsToInsert).stream()
                .forEach(idInserted -> dao.save(builder.bindingId(idInserted).build()));
    }

    @Test
    public void test_count_is_null_when_none_inserted() {
        //given no service added
        //when count asked
        int count = countServices();
        //then result is null
        assertThat(count, is(equalTo(0)));
    }

    @Test
    public void test_delete_all_purge_database() {
        //given we have some id in dao
        String[] idsToInsert = new String[]{"id1", "id2", "id3", "id4"};
        insertSomeIds(idsToInsert);

        //when we purge database
        dao.deleteAll();

        //then no service are contained in database
        assertThat(countServices(), is(equalTo(0)));
    }

    @Test
    public void test_insert_increase_count() {
        //given we insert some route binding
        String[] idsToInsert = new String[]{"id1", "id2", "id3"};
        insertSomeIds(idsToInsert);
        //when count is asked
        int count = countServices();
        //then result is null
        assertThat(count, is(equalTo(idsToInsert.length)));
    }

    @Test
    public void test_mass_delete_removes_element_in_dao() {
        //given we have some id in dao
        String[] idsToInsert = new String[]{"id1", "id2", "id3", "id4"};
        insertSomeIds(idsToInsert);

        //when we delete some elements
        dao.delete(dao.findAll(Arrays.asList(idsToInsert[0], idsToInsert[1])));

        //then the number in dao is decreased
        assertThat(countServices(), is(equalTo(idsToInsert.length - 2)));

    }

    @Test
    public void test_multiple_inserts_can_be_retrieved() {
        //given we have some data
        List<String> idsToInsert = Arrays.asList("id1", "id2", "id3");
        final RouteBinding.RouteBindingBuilder builder = RouteBinding.builder()
                .configurationId(CONFIGURATION_ID)
                .routeId(ROUTE_UID);
        List<RouteBinding> initialList = idsToInsert.stream()
                .map(s -> builder.bindingId(s).build())
                .collect(Collectors.toList());

        //when we insert data using the save all operation
        dao.save(initialList);

        //then the count return the exact value
        assertThat("Count should be equal to the amount inserted", countServices(),
                is(equalTo(initialList.size())));

        //and every element can be retrieved
        idsToInsert.forEach(id ->
                assertTrue("Each element should exist in DAO", dao.exists(id)));

        //and find all without filter return the good number of object
        long count = StreamSupport.stream(dao.findAll().spliterator(), false).count();
        assertThat("Retrieving all elements should return the same quantity", count, is(equalTo(initialList.size())));

        //and find all with filter return objects that are inserted

        StreamSupport.stream(dao.findAll(idsToInsert).spliterator(), false)
                .forEach(routeBinding ->
                        assertTrue("Retrieved element should be the same as initial element",
                                initialList.contains(routeBinding)));

    }

    @Test
    public void test_object_found_is_equal_to_the_one_saved() {
        //given we insert an object
        String bindingId = "bidingIdEquality";
        RouteBinding original = RouteBinding.builder()
                .bindingId(bindingId)
                .configurationId(CONFIGURATION_ID)
                .routeId(ROUTE_UID)
                .build();
        dao.save(original);
        //when we retrieve the object
        RouteBinding binding = dao.findOne(bindingId);

        //then object is not null
        assertFalse("Service binding should have been found", binding == null);
        //and all field are unchanged
        assertThat(binding.getConfigurationId(), is(equalTo(CONFIGURATION_ID)));
        assertThat(binding.getRouteId(), is(equalTo(ROUTE_UID)));
        assertThat(binding, is(equalTo(original)));
        assertTrue("Succeed in getting a binding that does not exist", dao.findOne("testGetServiceFail") == null);

    }

    @Test
    public void test_single_delete_by_element_removes_element_in_dao() {
        //given we have some id in dao
        String[] idsToInsert = new String[]{"id1", "id2", "id3", "id4"};
        insertSomeIds(idsToInsert);

        //when we delete an element by element
        dao.delete(dao.findOne(idsToInsert[0]));

        //then the number in dao is decreased
        assertThat(countServices(), is(equalTo(idsToInsert.length - 1)));
    }

    @Test
    public void test_single_delete_by_id_removes_element_in_dao() {
        //given we have some id in dao
        String[] idsToInsert = new String[]{"id1", "id2", "id3", "id4"};
        insertSomeIds(idsToInsert);

        //when we delete an element
        dao.delete(idsToInsert[0]);

        //then the number in dao is decreased
        assertThat(countServices(), is(equalTo(idsToInsert.length - 1)));

    }

}