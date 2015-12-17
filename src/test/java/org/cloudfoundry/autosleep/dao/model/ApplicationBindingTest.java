package org.cloudfoundry.autosleep.dao.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class ApplicationBindingTest {

    private static final String SERVICE_BINDING_ID = UUID.randomUUID().toString();
    private static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();
    private static final String APP_ID = UUID.randomUUID().toString();

    private ApplicationBinding getNewBinding() {
        return ApplicationBinding.builder()
                .serviceBindingId(SERVICE_BINDING_ID)
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .applicationId(APP_ID)
                .build();
    }

    @Test
    public void testEquals() throws Exception {
        assertFalse(getNewBinding().equals(null));
        assertFalse(getNewBinding().equals("toto"));
        assertTrue(getNewBinding().equals(getNewBinding()));
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(getNewBinding().hashCode() == getNewBinding().hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(getNewBinding().toString());
    }

}