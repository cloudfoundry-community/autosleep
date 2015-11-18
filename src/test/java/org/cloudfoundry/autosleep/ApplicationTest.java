package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.PostConstruct;

/**
 * Created by ben on 06/11/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationTest.MockClientConfiguration.class, Application.class})
@WebAppConfiguration
public class ApplicationTest {


    @Configuration
    @Slf4j
    public static class MockClientConfiguration {
        @PostConstruct
        public void initClientEnvironment() {
            log.debug("initClientEnvironment - setting properties");
            System.setProperty("cf.client.target.endpoint", "http://somewhere.org");
            System.setProperty("cf.client.skip.ssl.validation", "true");
            System.setProperty("cf.client.username", "username");
            System.setProperty("cf.client.password", "password");
            System.setProperty("cf.client.clientId", "clientId");
            System.setProperty("cf.client.clientId", "clientSecret");
        }
    }


    @Test
    public void testDummy() {
    }
}
