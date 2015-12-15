package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.PostConstruct;
import java.util.UUID;

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
            System.setProperty(Config.EnvKey.CF_ENDPOINT, "http://somewhere.org");
            System.setProperty(Config.EnvKey.CF_SKIP_SSL_VALIDATION, "true");
            System.setProperty(Config.EnvKey.CF_USERNAME, "username");
            System.setProperty(Config.EnvKey.CF_PASSWORD, "password");
            System.setProperty(Config.EnvKey.CF_CLIENT_ID, "clientId");
            System.setProperty(Config.EnvKey.CF_CLIENT_SECRET, "clientSecret");
            System.setProperty(Config.EnvKey.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY,
                    BeanGenerator.getSampleVcapApplication(UUID.randomUUID(), "autosleep",
                            "http://somewhere-else.org"));
        }
    }


    @Test
    public void testDummy() {
    }
}
