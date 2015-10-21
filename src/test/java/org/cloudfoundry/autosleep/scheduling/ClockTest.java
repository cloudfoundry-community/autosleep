package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {Clock.class})
public class ClockTest {

    private static final Duration PERIOD = Duration.ofMillis(500);
    private static final String TEST_ID = "93847";

    @Autowired
    protected org.cloudfoundry.autosleep.scheduling.Clock clock;
    protected LocalDateTime lastLaunchTime;
    private int count = 0;

    private Runnable runnable = () -> {
        log.debug("Ticking");
        count++;
        lastLaunchTime = LocalDateTime.now();
    };


    @Test(timeout = 5000)
    public void testStartTimer() throws Exception {
        clock.startTimer(TEST_ID, Duration.ofSeconds(0), PERIOD, runnable);
        while (count < 3) {
            Thread.sleep(200);
        }
        //if we reach this line before the timeout, the test worked
        assert true;
    }


    @Test
    public void testStopTimer() throws Exception {
        clock.startTimer(TEST_ID, Duration.ofSeconds(0), PERIOD, runnable);
        Thread.sleep(1500);
        log.debug("last launch {} is after {} ",lastLaunchTime,LocalDateTime.now().minus(PERIOD) );
        assertTrue(lastLaunchTime.isAfter(LocalDateTime.now().minus(PERIOD)));
        clock.stopTimer(TEST_ID);
        Thread.sleep(1000);
        assertFalse(lastLaunchTime.isAfter(LocalDateTime.now().minus(PERIOD)));
    }
}
