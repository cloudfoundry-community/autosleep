package org.cloudfoundry.autosleep.client;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.client.CloudFoundryException.Type;
import org.cloudfoundry.autosleep.client.model.AppEntity;
import org.cloudfoundry.autosleep.client.model.CloudfoundryObject;
import org.cloudfoundry.autosleep.client.model.OrganizationEntity;
import org.cloudfoundry.autosleep.client.model.SpaceEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.constraints.AssertTrue;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {CloudFoundryApiClient.class, ClientConfigurationBuilder.class})
public class CloudFoundryApiClientTest {

    @Autowired
    private ClientConfigurationBuilder clientConfiguration;

    @Autowired
    private CloudFoundryApiClientService client;


    @Before
    public void initConfiguration() throws CloudFoundryException {
        if (clientConfiguration.getTargetEndpoint().equals("") || clientConfiguration.getUsername().equals("")
                || clientConfiguration.getClientId().equals("") || clientConfiguration.getPassword().equals("")) {
            log.debug("initConfiguration - test skipped");
        } else {
            if (client.getCredentials() == null) {
                log.debug("initConfiguration - start");
                client.initCredential(clientConfiguration.getUsername(), clientConfiguration.getPassword());
                assertThat(client.getCredentials(), is(notNullValue()));
                assertThat(client.getCredentials().getAccessToken(), is(notNullValue()));
                assertThat(client.getCredentials().getRefreshToken(), is(notNullValue()));
                assertThat(client.getCredentials().getScope(), is(notNullValue()));
            }
        }

    }


    @Test
    public void testSetCredentials() throws CloudFoundryException {
        if (client.getCredentials() != null) {
            log.debug("testSetCredentials - start");
            try {
                client.setCredentials(client.getCredentials().getRefreshToken() + "something");
                fail("Bad refresh token should have failed");
            } catch (CloudFoundryException c) {
                assertThat(c.getType(), is(equalTo(Type.CallError)));
                assertThat(c.getStatusCode(), is(equalTo(401)));
            }
            client.setCredentials(client.getCredentials().getRefreshToken());
            assertThat(client.getCredentials(), is(notNullValue()));
            assertThat(client.getCredentials().getAccessToken(), is(notNullValue()));
            assertThat(client.getCredentials().getRefreshToken(), is(notNullValue()));
            assertThat(client.getCredentials().getScope(), is(notNullValue()));
        } else {
            log.warn("testSetCredentials test skipped");
        }

    }

    @Test
    public void testLogout() {
        if (client.getCredentials() != null) {
            log.debug("testLogout - start");
            client.logout();
            assertThat(client.getCredentials(), is(nullValue()));
        } else {
            log.warn("testLogout test skipped");
        }
    }

    @Test
    public void testGetOrganizations() throws CloudFoundryException {
        if (client.getCredentials() != null) {
            log.debug("testGetOrganizations - start");
            AtomicInteger cptOrganization = new AtomicInteger(0);
            client.readOrganizations(cloundFoundryOrganization -> {
                log.debug(" - organization - {}", cloundFoundryOrganization.getEntity().getName());
                cptOrganization.incrementAndGet();
                return true;
            });
            assertTrue("No organization found", cptOrganization.get() > 0);
        } else {
            log.warn("testGetOrganizations test skipped");
        }
    }

    @Test
    public void testGetSpaces() throws CloudFoundryException {
        if (client.getCredentials() != null) {
            log.debug("testGetSpaces - start");
            AtomicInteger cptSpaces = new AtomicInteger(0);
            client.readOrganizations(cloundFoundryOrganization -> {
                client.readSpaces(cloundFoundryOrganization,
                        cloudFoundrySpace -> {
                            log.debug(" - space - {} - {}", cloundFoundryOrganization.getEntity().getName(),
                                    cloudFoundrySpace.getEntity().getName());
                            cptSpaces.incrementAndGet();
                            return true;
                        });
                return true;
            });
            assertTrue("No space found", cptSpaces.get() > 0);
        } else {
            log.warn("testGetSpaces test skipped");
        }
    }

    @Test
    public void testGetApps() throws CloudFoundryException {
        if (client.getCredentials() != null) {
            log.debug("testGetSpaces - start");
            AtomicInteger cptApps = new AtomicInteger(0);
            client.readOrganizations(cloundFoundryOrganization -> {
                client.readSpaces(cloundFoundryOrganization,
                        cloudFoundrySpace -> {
                            client.readApps(cloudFoundrySpace,
                                    cloudFoundryApp -> {
                                        log.debug(" - app - {} - {} - {}- {}",
                                                cloundFoundryOrganization.getEntity().getName(),
                                                cloudFoundrySpace.getEntity().getName(),
                                                cloudFoundryApp.getEntity().getName(),
                                                cloudFoundryApp.getEntity().getNbInstances());
                                        cptApps.incrementAndGet();
                                        return true;
                                    });
                            return true;
                        });
                return true;
            });
            assertTrue("No app found", cptApps.get() > 0);
        } else {
            log.warn("testGetApps test skipped");
        }
    }

    @Test
    public void testStartApp() throws CloudFoundryException {
        if (client.getCredentials() != null) {
            client.readOrganizations(cloundFoundryOrganization -> {
                client.readSpaces(cloundFoundryOrganization,
                        cloudFoundrySpace -> {
                            client.readApps(cloudFoundrySpace,
                                    cloudFoundryApp -> {
                                        log.debug(" - starting app - {} - {} - {}",
                                                cloundFoundryOrganization.getEntity().getName(),
                                                cloudFoundrySpace.getEntity().getName(),
                                                cloudFoundryApp.getEntity().getName());
                                        client.startApp(cloudFoundryApp);
                                        return false;
                                    });
                            return false;
                        });
                return false;
            });
        }
    }

    @Test
    public void testStopApp() throws CloudFoundryException {
        if (client.getCredentials() != null) {
            client.readOrganizations(cloundFoundryOrganization -> {
                client.readSpaces(cloundFoundryOrganization,
                        cloudFoundrySpace -> {
                            client.readApps(cloudFoundrySpace,
                                    cloudFoundryApp -> {
                                        log.debug(" - stopping app - {} - {} - {}",
                                                cloundFoundryOrganization.getEntity().getName(),
                                                cloudFoundrySpace.getEntity().getName(),
                                                cloudFoundryApp.getEntity().getName());
                                        client.stopApp(cloudFoundryApp);
                                        return false;
                                    });
                            return false;
                        });
                return false;
            });
        }
    }
}