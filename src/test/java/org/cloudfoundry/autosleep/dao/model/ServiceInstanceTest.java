package org.cloudfoundry.autosleep.dao.model;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class ServiceInstanceTest {

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
        assertFalse(new ASServiceInstance(createRequest).equals(null));
        assertFalse(new ASServiceInstance(createRequest).equals("toto"));
        assertTrue(new ASServiceInstance(createRequest).equals(new ASServiceInstance(createRequest)));
        assertTrue(new ASServiceInstance(updateRequest).equals(new ASServiceInstance(updateRequest)));
        assertTrue(new ASServiceInstance(deleteRequest).equals(new ASServiceInstance(deleteRequest)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(new ASServiceInstance(createRequest).hashCode()
                == new ASServiceInstance(createRequest).hashCode());
        assertTrue(new ASServiceInstance(updateRequest).hashCode()
                == new ASServiceInstance(updateRequest).hashCode());
        assertTrue(new ASServiceInstance(deleteRequest).hashCode()
                == new ASServiceInstance(deleteRequest).hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(new ASServiceInstance(createRequest).toString());
    }

}