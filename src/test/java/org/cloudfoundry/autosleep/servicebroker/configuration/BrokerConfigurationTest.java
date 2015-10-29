package org.cloudfoundry.autosleep.servicebroker.configuration;


import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = BrokerConfiguration.class)
public class BrokerConfigurationTest {

    @Autowired
    private BrokerApiVersion brokerApiVersion;

    @Test
    public void testBrokerVersion() {
        assertThat(brokerApiVersion.getApiVersion(), is(equalTo("*")));
    }

}