package org.cloudfoundry.autosleep.servicebroker.service;

import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AutosleepCatalogService.class)
public class AutosleepCatalogServiceTest {


    @Autowired
    private CatalogService catalogService;


    @Test
    public void testGetCatalog() {
        assertThat(catalogService.getCatalog().getServiceDefinitions().size(), is(equalTo(1)));
        ServiceDefinition serviceDefinition = catalogService.getCatalog().getServiceDefinitions().get(0);
        assertThat(serviceDefinition.getId(), is(equalTo("autosleep")));
        assertThat(serviceDefinition.getPlans().size(), is(equalTo(1)));
        assertTrue(serviceDefinition.isBindable());
        assertFalse(serviceDefinition.isPlanUpdateable());
        Plan plan = serviceDefinition.getPlans().get(0);
        UUID.fromString(plan.getId());
        assertTrue(plan.isFree());
    }

    @Test
    public void testGetServiceDefinition() throws Exception {
        assertThat(catalogService.getCatalog().getServiceDefinitions().size(), is(equalTo(1)));
        ServiceDefinition serviceDefinition = catalogService.getCatalog().getServiceDefinitions().get(0);
        assertThat(catalogService.getServiceDefinition(serviceDefinition.getId()), is(notNullValue()));
        assertThat(catalogService.getServiceDefinition(serviceDefinition.getId() + "-fake"), is(nullValue()));
    }
}
