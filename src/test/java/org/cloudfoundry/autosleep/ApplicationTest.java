/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
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
            System.setProperty(Config.EnvKey.CF_HOST, "somewhere.org");
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
