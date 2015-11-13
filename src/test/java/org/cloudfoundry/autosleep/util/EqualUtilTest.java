package org.cloudfoundry.autosleep.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class EqualUtilTest {

    @SuppressWarnings("AccessStaticViaInstance")
    @Test
    public void testAreEquals() throws Exception {
        EqualUtil instance = new EqualUtil();
        assertTrue(instance.areEquals("toto", "toto"));
        assertTrue(EqualUtil.areEquals("toto", "toto"));
        assertTrue(EqualUtil.areEquals(null, null));
        assertFalse(EqualUtil.areEquals(null, "toto"));
        assertFalse(EqualUtil.areEquals("toto", null));
        assertFalse(EqualUtil.areEquals("toto", "tata"));
    }
}