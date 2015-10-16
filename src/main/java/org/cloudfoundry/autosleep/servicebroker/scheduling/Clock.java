package org.cloudfoundry.autosleep.servicebroker.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**Timer start.
     * @param id            task id, will be used to stop timer
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @param unit         the time unit of the initialDelay and period parameters
     */
    public void startTimer(String id, long initialDelay, long period, TimeUnit unit, Runnable action) {
        log.debug("startTimer - service {}", id);
        ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(action, initialDelay, period, unit);
        tasks.put(id, handle);
    }

    /**Timer stop.
     * @param id id given when started the task
     */
    public void stopTimer(String id) {
        log.debug("stopTimer - service {}", id);
        tasks.get(id).cancel(true);
    }
}
