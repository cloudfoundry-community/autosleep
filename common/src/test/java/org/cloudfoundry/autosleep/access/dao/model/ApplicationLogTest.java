package org.cloudfoundry.autosleep.access.dao.model;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class ApplicationLogTest {

    private String generateRandomString(int length) {
        char[] charArray = new char[length];
        Arrays.fill(charArray, ' ');
        return new String(charArray);
    }

    @Test
    public void long_messages_get_truncated() {
        String message = generateRandomString(255);
        ApplicationInfo.DiagnosticInfo.ApplicationLog applicationLog = ApplicationInfo.DiagnosticInfo
                .ApplicationLog.builder()
                .message(message)
                .messageType("messageType")
                .sourceInstance("sourceInstance")
                .sourceType("sourceType")
                .timestamp(Instant.EPOCH)
                .build();
        assertTrue(applicationLog.getMessage().length() == 254);
    }

}