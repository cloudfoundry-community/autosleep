package org.cloudfoundry.autosleep.dao.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class ApplicationBindingTest {

    private static final String SERVICE_DEFINITION_ID = UUID.randomUUID().toString();
    private static final String SERVICE_ID = UUID.randomUUID().toString();
    private static final String APP = UUID.randomUUID().toString();
    private static final String LOG_URL = UUID.randomUUID().toString();
    private static final String PLAN = UUID.randomUUID().toString();

    private ApplicationBinding getNewBinding() {
        return new ApplicationBinding(SERVICE_DEFINITION_ID,SERVICE_ID,new HashMap<>(),APP,LOG_URL);
    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes"})
    @Test
    public void testEquals() throws Exception {
        assertFalse(getNewBinding().equals(null));
        assertFalse(getNewBinding().equals("toto"));
        assertTrue(getNewBinding().equals(getNewBinding()));
        assertTrue(getNewBinding().equals(getNewBinding()));
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