package org.cloudfoundry.autosleep.access.dao.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ApplicationLogTest {

    @Test
    public void long_messages_get_truncated() {
        String message = aStringOfLength(255);
        ApplicationInfo.DiagnosticInfo.ApplicationLog applicationLog = new ApplicationInfo.DiagnosticInfo.ApplicationLog(message, "sourcetype", "sourceid", "sourceName", 0L);
        assertTrue(applicationLog.getMessage().length() == 254);
    }

    private String aStringOfLength(int length) {
        char[] charArray = new char[length];
        Arrays.fill(charArray, ' ');
        return new String(charArray);
    }

}