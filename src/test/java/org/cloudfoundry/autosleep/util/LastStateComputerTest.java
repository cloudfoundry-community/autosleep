package org.cloudfoundry.autosleep.util;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class LastStateComputerTest {
    private final Instant yesterday = Instant.now().minus(Duration.ofDays(1));
    private final Instant now = Instant.now();

    @Test
    public void testComputeLastDate() throws Exception {
        assertThat(LastDateComputer.INSTANCE.computeLastDate(now, yesterday), is(equalTo(now)));
        assertThat(LastDateComputer.INSTANCE.computeLastDate(yesterday, now), is(equalTo(now)));
        assertThat(LastDateComputer.INSTANCE.computeLastDate(null, now), is(equalTo(now)));
        assertThat(LastDateComputer.INSTANCE.computeLastDate(now, null), is(equalTo(now)));
        assertThat(LastDateComputer.INSTANCE.computeLastDate(null, null), is(nullValue()));
    }
}