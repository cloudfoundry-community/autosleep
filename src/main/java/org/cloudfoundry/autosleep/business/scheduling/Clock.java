package org.cloudfoundry.autosleep.business.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Config.NB_THREAD_FOR_TASK);

    /**
     * Schedule a Runnable to be run after a certain delay.
     *
     * @param id       task id, will be used to remove it
     * @param duration the time to wait before execution
     * @param action   Runnable to call
     */
    public void scheduleTask(String id, Duration duration, Runnable action) {
        log.debug("scheduleTask - task {}", id);
        ScheduledFuture<?> handle = scheduler.schedule(action, duration.toMillis(), TimeUnit.MILLISECONDS);
        tasks.put(id, handle);
    }


    /**
     * Remove a task by its id.
     *
     * @param id task id, will be used to cancel it
     */
    public void removeTask(String id) {
        log.debug("removeTask - task {}", id);
        tasks.remove(id);
    }

    /**
     * Access to the task ids.
     *
     * @return a read-only set containing the ids of the current tasks
     */
    public Set<String> listTaskIds() {
        return Collections.unmodifiableSet(tasks.keySet());
    }

}
