package org.cloudfoundry.autosleep.dao.model;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class AutosleepServiceInstanceTest {

    private static final String SERVICE_DEFINITION_ID = UUID.randomUUID().toString();
    private static final String SERVICE_ID = UUID.randomUUID().toString();
    private static final String ORG = UUID.randomUUID().toString();
    private static final String SPACE = UUID.randomUUID().toString();
    private static final String PLAN = UUID.randomUUID().toString();

    private CreateServiceInstanceRequest createRequest;

    private UpdateServiceInstanceRequest updateRequest;

    private DeleteServiceInstanceRequest deleteRequest;

    private Duration duration = Duration.ofMinutes(15);

    private Pattern exclude = Pattern.compile(".*");

    private boolean noOptOut = true;

    private String secretHash = "someSecret";


    @Before
    public void init() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Config.ServiceInstanceParameters.IDLE_DURATION, duration);
        parameters.put(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, exclude);
        parameters.put(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, noOptOut);
        parameters.put(Config.ServiceInstanceParameters.SECRET, secretHash);

        createRequest = new CreateServiceInstanceRequest(
                SERVICE_DEFINITION_ID, PLAN, ORG, SPACE, parameters).withServiceInstanceId(SERVICE_ID);
        updateRequest = new UpdateServiceInstanceRequest(PLAN, parameters)
                .withInstanceId(SERVICE_ID);
        deleteRequest = new DeleteServiceInstanceRequest(SERVICE_ID,
                SERVICE_DEFINITION_ID,
                PLAN);
    }

    @Test
    public void testInstanciation() {
        AutosleepServiceInstance serviceInstance = new AutosleepServiceInstance(createRequest);
        assertThat(serviceInstance.getInterval(), is(equalTo(duration)));
        assertThat(serviceInstance.getExcludeNames(), is(equalTo(exclude)));
        assertThat(serviceInstance.isNoOptOut(), is(equalTo(noOptOut)));
        assertThat(serviceInstance.getSecretHash(), is(equalTo(secretHash)));
    }

    @Test
    public void testUpdate() {
        AutosleepServiceInstance serviceInstance = new AutosleepServiceInstance(createRequest);
        serviceInstance.updateFromParameters(Collections
                .singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, Duration.ofSeconds(2)));
        assertThat(serviceInstance.getInterval(), is(not(equalTo(duration))));
        assertThat(serviceInstance.getExcludeNames(), is(equalTo(exclude)));
        assertThat(serviceInstance.isNoOptOut(), is(equalTo(noOptOut)));
        assertThat(serviceInstance.getSecretHash(), is(equalTo(secretHash)));

        serviceInstance = new AutosleepServiceInstance(createRequest);
        serviceInstance.updateFromParameters(Collections
                .singletonMap(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, Pattern.compile("..*")));
        assertThat(serviceInstance.getInterval(), is(equalTo(duration)));
        assertThat(serviceInstance.getExcludeNames(), is(not(equalTo(exclude))));
        assertThat(serviceInstance.isNoOptOut(), is(equalTo(noOptOut)));
        assertThat(serviceInstance.getSecretHash(), is(equalTo(secretHash)));

        serviceInstance = new AutosleepServiceInstance(createRequest);
        serviceInstance.updateFromParameters(Collections
                .singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, !noOptOut));
        assertThat(serviceInstance.getInterval(), is(equalTo(duration)));
        assertThat(serviceInstance.getExcludeNames(), is(equalTo(exclude)));
        assertThat(serviceInstance.isNoOptOut(), is(not(equalTo(noOptOut))));
        assertThat(serviceInstance.getSecretHash(), is(equalTo(secretHash)));

    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes"})
    @Test
    public void testEquals() throws Exception {
        assertFalse(new AutosleepServiceInstance(createRequest).equals(null));
        assertFalse(new AutosleepServiceInstance(createRequest).equals("toto"));
        assertTrue(new AutosleepServiceInstance(createRequest).equals(new AutosleepServiceInstance(createRequest)));
        assertTrue(new AutosleepServiceInstance(updateRequest).equals(new AutosleepServiceInstance(updateRequest)));
        assertTrue(new AutosleepServiceInstance(deleteRequest).equals(new AutosleepServiceInstance(deleteRequest)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(new AutosleepServiceInstance(createRequest).hashCode()
                == new AutosleepServiceInstance(createRequest).hashCode());
        assertTrue(new AutosleepServiceInstance(updateRequest).hashCode()
                == new AutosleepServiceInstance(updateRequest).hashCode());
        assertTrue(new AutosleepServiceInstance(deleteRequest).hashCode()
                == new AutosleepServiceInstance(deleteRequest).hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(new AutosleepServiceInstance(createRequest).toString());
    }
}