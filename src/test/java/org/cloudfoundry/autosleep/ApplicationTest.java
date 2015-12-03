package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
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
            System.setProperty(Config.EnvKey.cfEndPoint, "http://somewhere.org");
            System.setProperty(Config.EnvKey.cfSkipSSLValidation, "true");
            System.setProperty(Config.EnvKey.cfUserName, "username");
            System.setProperty(Config.EnvKey.cfPassword, "password");
            System.setProperty(Config.EnvKey.cfClientId, "clientId");
            System.setProperty(Config.EnvKey.cfClientSecret, "clientSecret");
        }
    }


    @Test
    public void testDummy() {
    }
}
