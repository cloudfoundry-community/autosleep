package org.cloudfoundry.autosleep.remote;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;


public class ApplicationInfoTest {

    private final Instant yesterday = Instant.now().minus(Duration.ofDays(1));
    private final Instant now = Instant.now();

    @Test
    public void testGetLastEventTime() throws Exception {
        ApplicationInfo info = new ApplicationInfo(yesterday, now,null);
        assertThat("Most recent date should be last log", info.getLastActionDate(), is(equalTo(info.getLastLog())));
        assertThat("Last event should return most recent date", info.getLastActionDate(), is(equalTo(now)));

        info = new ApplicationInfo(now, yesterday,null);
        assertThat("Last event should return most recent date", info.getLastActionDate(), is(equalTo(now)));
        assertThat("Most recent date should be last deployed", info.getLastEvent(),
                is(equalTo(info.getLastActionDate())));

        info = new ApplicationInfo(now, null, null);
        assertThat("Last event should not be null", info.getLastActionDate(), is(equalTo(now)));

        info = new ApplicationInfo(null, now, null);
        assertThat("Last event should not be null", info.getLastActionDate(), is(equalTo(now)));

        info = new ApplicationInfo(null, null, null);
        assertThat("Last event should rbe null", info.getLastActionDate(), is(nullValue()));

    }
}