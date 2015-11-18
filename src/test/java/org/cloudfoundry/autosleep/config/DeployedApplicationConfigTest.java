package org.cloudfoundry.autosleep.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class DeployedApplicationConfigTest {

    private static final UUID APP_ID = UUID.randomUUID();

    @Mock
    private Environment environment;

    @InjectMocks
    private DeployedApplicationConfig config;

    @Test
    public void testLoadCurrentDeployment() throws Exception {
        when(environment.getProperty(eq(DeployedApplicationConfig.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY)))
                .thenReturn(null);
        Deployment deployment = config.loadCurrentDeployment();
        assertThat(deployment, is(nullValue()));

        when(environment.getProperty(eq(DeployedApplicationConfig.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY)))
                .thenReturn(getSampleVcapApplication());
        deployment = config.loadCurrentDeployment();
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getApplicationId(), is(equalTo(APP_ID)));

    }

    private String getSampleVcapApplication() {
        return "{\"limits\":{\"mem\":1024,\"disk\":1024,\"fds\":16384}," +
                "\"application_id\":\"" + APP_ID.toString() + "\"," +
                "\"application_version\":\"b546c9d4-8885-4d50-a855-490ddb5b5a1c\"," +
                "\"application_name\":\"autosleep-app\"," +
                "\"application_uris\":[\"autosleep-app-ben.cf.ns.nd-paas.itn.ftgroup\"," +
                "\"autosleep-nonnational-artotype.cf.ns.nd-paas.itn.ftgroup\"," +
                "\"autosleep.cf.ns.nd-paas.itn.ftgroup\"]," +
                " \"version\":\"b546c9d4-8885-4d50-a855-490ddb5b5a1c\"," +
                "\"name\":\"autosleep-app\"," +
                "\"space_name\":\"autosleep\"" +
                ",\"space_id\":\"2d745a4b-67e3-4398-986e-2adbcf8f7ec9\"," +
                "\"uris\":[\"autosleep-app-ben.cf.ns.nd-paas.itn.ftgroup\"," +
                "\"autosleep-nonnational-artotype.cf.ns.nd-paas.itn.ftgroup\"," +
                "\"autosleep.cf.ns.nd-paas.itn.ftgroup\"]" +
                ",\"users\":null," +
                "\"instance_id\":\"7984a682cab9447891674f862299c77f\"," +
                "\"instance_index\":0," +
                "\"host\":\"0.0.0.0\"," +
                "\"port\":61302," +
                "\"started_at\":\"2015-11-18 15:49:06 +0000\"," +
                "\"started_at_timestamp\":1447861746," +
                "\"start\":\"2015-11-18 15:49:06 +0000\"," +
                "\"state_timestamp\":1447861746" +
                "}";
    }
}