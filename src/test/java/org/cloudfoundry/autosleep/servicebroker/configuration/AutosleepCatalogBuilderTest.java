package org.cloudfoundry.autosleep.servicebroker.configuration;

import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AutosleepCatalogBuilder.class)
public class AutosleepCatalogBuilderTest {


    @Autowired
    private Catalog catalog;


    @Test
    public void testBuildCatalog() {
        assertThat(catalog.getServiceDefinitions().size(), is(equalTo(1)));
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        assertThat(serviceDefinition.getId(), is(equalTo("autosleep")));
        assertThat(serviceDefinition.getPlans().size(), is(equalTo(1)));
        assertTrue(serviceDefinition.isBindable());
        assertFalse(serviceDefinition.isPlanUpdateable());
        Plan plan = serviceDefinition.getPlans().get(0);
        UUID.fromString(plan.getId());
        assertTrue(plan.isFree());
    }


}
