package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ClockTest {

    private static final Duration PERIOD = Duration.ofMillis(200);

    private static final Duration SLEEP_TIME_BEFORE_TEST = PERIOD.dividedBy(3);

    private static final String TEST_ID = "93847";

    protected Clock clock = new Clock();

    @Mock
    private Runnable runnable;


    @Test
    public void testStartTask() throws Exception {
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        Thread.sleep(SLEEP_TIME_BEFORE_TEST.toMillis());
        verify(runnable, never()).run();
        Thread.sleep(PERIOD.toMillis());
        verify(runnable, times(1)).run();
        Thread.sleep(PERIOD.toMillis());
        verify(runnable, times(1)).run();
    }

    @Test
    public void testRemoveTask() throws Exception {
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        clock.removeTask(TEST_ID);
        assertThat(clock.listTaskIds().size(), is(equalTo(0)));

    }

    @Test
    public void testListTaskIds() throws Exception {
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        Set<String> taskIds = clock.listTaskIds();
        assertThat(taskIds, is(notNullValue()));
        assertThat(taskIds.size(), is(equalTo(1)));
        assertTrue(taskIds.contains(TEST_ID));
    }
}
