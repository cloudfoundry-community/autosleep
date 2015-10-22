package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Scope(value = "singleton")
public class Clock {

    //TODO redis that
    private final Map<String/*taskId*/, ScheduledFuture<?>> tasks = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    /**
     * Timer start.
     *
     * @param id           task id, will be used to stop timer
     * @param initialDelay the time to delay first execution
     * @param period      the time to wait between executions
     * @param action       Runnable to call
     */
    public void startTimer(String id, Duration initialDelay, Duration period,  Runnable action) {
        log.debug("startTimer - task {}", id);
        ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(action, initialDelay.toMillis(),
                period.toMillis(), TimeUnit.MILLISECONDS);
        tasks.put(id, handle);
    }

    /**
     * Schedule a Runnable to be run after a certain delay.
     *
     * @param id     task id, will be used to cancel it
     * @param duration  the time to wait before execution
     * @param action Runnable to call
     */
    public void scheduleTask(String id, Duration duration, Runnable action) {
        log.debug("schedule - task {}", id);
        ScheduledFuture<?> handle = scheduler.schedule(action, duration.toMillis(), TimeUnit.MILLISECONDS);
        tasks.put(id, handle);
    }

    /**
     * Timer stop.
     *
     * @param id id given when started the task
     */
    public void stopTimer(String id) {
        log.debug("stopTimer - service {}", id);
        tasks.get(id).cancel(true);
    }
}
