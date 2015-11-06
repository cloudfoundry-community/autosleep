package org.cloudfoundry.autosleep.servicebroker.model;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class AutoSleepServiceInstanceTest {

    private static final String SERVICE_DEFINITION_ID = UUID.randomUUID().toString();
    private static final String SERVICE_ID = UUID.randomUUID().toString();
    private static final String ORG = UUID.randomUUID().toString();
    private static final String SPACE = UUID.randomUUID().toString();
    private static final String PLAN = UUID.randomUUID().toString();

    private final CreateServiceInstanceRequest createRequest = new CreateServiceInstanceRequest(
            SERVICE_DEFINITION_ID, PLAN, ORG, SPACE, new HashMap<>()).withServiceInstanceId(SERVICE_ID);
    private final UpdateServiceInstanceRequest updateRequest = new UpdateServiceInstanceRequest(PLAN)
            .withInstanceId(SERVICE_ID);
    private final DeleteServiceInstanceRequest deleteRequest = new DeleteServiceInstanceRequest(SERVICE_ID,
            SERVICE_DEFINITION_ID,
            PLAN);

    @SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes"})
    @Test
    public void testEquals() throws Exception {
        assertFalse(new AutoSleepServiceInstance(createRequest).equals(null));
        assertFalse(new AutoSleepServiceInstance(createRequest).equals("toto"));
        assertTrue(new AutoSleepServiceInstance(createRequest).equals(new AutoSleepServiceInstance(createRequest)));
        assertTrue(new AutoSleepServiceInstance(updateRequest).equals(new AutoSleepServiceInstance(updateRequest)));
        assertTrue(new AutoSleepServiceInstance(deleteRequest).equals(new AutoSleepServiceInstance(deleteRequest)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(new AutoSleepServiceInstance(createRequest).hashCode()
                == new AutoSleepServiceInstance(createRequest).hashCode());
        assertTrue(new AutoSleepServiceInstance(updateRequest).hashCode()
                == new AutoSleepServiceInstance(updateRequest).hashCode());
        assertTrue(new AutoSleepServiceInstance(deleteRequest).hashCode()
                == new AutoSleepServiceInstance(deleteRequest).hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(new AutoSleepServiceInstance(createRequest).toString());
    }

}