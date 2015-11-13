package org.cloudfoundry.autosleep.dao.model;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class AustosleepServiceInstanceTest {

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