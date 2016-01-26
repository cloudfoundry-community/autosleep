package org.cloudfoundry.autosleep.util;

import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
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
    private final ApplicationInfo.DiagnosticInfo.ApplicationLog oldLog = BeanGenerator.createAppLog(yesterday);
    private final ApplicationInfo.DiagnosticInfo.ApplicationLog recentLog = BeanGenerator.createAppLog(now);
    private final ApplicationInfo.DiagnosticInfo.ApplicationEvent oldEvent = BeanGenerator.createCloudEvent(yesterday);
    private final ApplicationInfo.DiagnosticInfo.ApplicationEvent recentEvent = BeanGenerator.createCloudEvent(now);

    @SuppressWarnings("AccessStaticViaInstance")
    @Test
    public void should_return_last_event_date_if_more_recent_than_log() throws Exception {
        //given that last event is more recent than last log
        ApplicationInfo.DiagnosticInfo.ApplicationLog log = oldLog;
        ApplicationInfo.DiagnosticInfo.ApplicationEvent event = recentEvent;

        //when asking for most recent activity
        Instant result = LastDateComputer.computeLastDate(log, event);

        //then it returns last event date
        assertThat(result, is(equalTo(event.getTimestamp())));
    }

    @Test
    public void should_return_last_log_date_if_more_recent_than_event() throws Exception {
        //given that last log is more recent than last event
        ApplicationInfo.DiagnosticInfo.ApplicationLog log = recentLog;
        ApplicationInfo.DiagnosticInfo.ApplicationEvent event = oldEvent;

        //when asking for most recent activity
        Instant result = LastDateComputer.computeLastDate(log, event);

        //then it returns last log date
        assertThat(result, is(equalTo(recentLog.getTimestamp())));
    }

    @Test
    public void should_return_last_event_if_last_log_null() throws Exception {
        //given that last log is null
        ApplicationInfo.DiagnosticInfo.ApplicationLog log = null;
        ApplicationInfo.DiagnosticInfo.ApplicationEvent event = recentEvent;

        //when asking for most recent activity
        Instant result = LastDateComputer.computeLastDate(log, event);

        //then it returns last event date
        assertThat(result, is(equalTo(event.getTimestamp())));
    }

    @Test
    public void should_return_last_log_if_last_recent_null() throws Exception {
        //given that last event is null
        ApplicationInfo.DiagnosticInfo.ApplicationLog log = oldLog;
        ApplicationInfo.DiagnosticInfo.ApplicationEvent event = null;

        //when asking for most recent activity
        Instant result = LastDateComputer.computeLastDate(log, event);

        //then it returns last log date
        assertThat(result, is(equalTo(log.getTimestamp())));
    }


    @Test
    public void should_return_null_when_both_null() throws Exception {
        //given that last event is null
        ApplicationInfo.DiagnosticInfo.ApplicationLog log = null;
        ApplicationInfo.DiagnosticInfo.ApplicationEvent event = null;

        //when asking for most recent activity
        Instant result = LastDateComputer.computeLastDate(log, event);

        //then it returns last log date
        assertThat(result, is(nullValue()));
    }
}