package org.cloudfoundry.autosleep.remote;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class ApplicationInfoTest {

    private final LocalDateTime yesterday = LocalDateTime.now().minus(Duration.ofDays(1));
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    public void testGetLastEventTime() throws Exception {
        ApplicationInfo info = new ApplicationInfo(yesterday, now);
        assertEquals("Most recent date should be last log", info.getLastLog(), info.getLastEventTime());
        assertEquals("Last event should return most recent date", info.getLastEventTime(), now);

        info = new ApplicationInfo(now, yesterday);
        assertEquals("Last event should return most recent date", info.getLastEventTime(), now);
        assertEquals("Most recent date should be last deployed", info.getLastDeployed(), info.getLastEventTime());

    }
}