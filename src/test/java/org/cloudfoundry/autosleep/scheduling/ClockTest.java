package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = org.cloudfoundry.autosleep.Application.class)
@WebIntegrationTest("server.port:0")//random port
@Slf4j
public class ClockTest {

    private static final long PERIOD_IN_MILLIS = 500;
    private static final String TEST_ID = "93847";

    @Autowired
    protected org.cloudfoundry.autosleep.scheduling.Clock clock;
    long lastLaunchTime;
    private int count = 0;

    private Runnable runnable = () -> {
        log.debug("Ticking");
        count++;
        lastLaunchTime = System.currentTimeMillis();
    };


    @Test(timeout = 5000)
    public void testStartTimer() throws Exception {
        clock.startTimer(TEST_ID, 0, PERIOD_IN_MILLIS, TimeUnit.MILLISECONDS, runnable);
        while (count < 3) {
            Thread.sleep(200);
        }
        //if we reach this line before the timeout, the test worked
        assert true;
    }


    @Test
    public void testStopTimer() throws Exception {
        clock.startTimer(TEST_ID, 0, PERIOD_IN_MILLIS, TimeUnit.MILLISECONDS, runnable);
        Thread.sleep(1000);
        assertTrue(lastLaunchTime >= System.currentTimeMillis() - PERIOD_IN_MILLIS);
        clock.stopTimer(TEST_ID);
        Thread.sleep(1000);
        assertFalse(lastLaunchTime >= System.currentTimeMillis() - PERIOD_IN_MILLIS);
    }
}
