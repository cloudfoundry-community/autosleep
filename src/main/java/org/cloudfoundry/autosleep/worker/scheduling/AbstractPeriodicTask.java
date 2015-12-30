package org.cloudfoundry.autosleep.worker.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@AllArgsConstructor
public abstract class AbstractPeriodicTask implements Runnable {

    private Clock clock;

    @Getter(value = AccessLevel.PROTECTED)
    private Duration period;

    public void start(Duration delay) {
        log.debug("start - {}", delay);
        clock.scheduleTask(getTaskId(), delay == null ? Duration.ofSeconds(0) : delay, this);
    }

    public void startNow() {
        start(Duration.ofSeconds(0));
    }

    public Instant reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(getTaskId(), delta, this);
        return Instant.now().plus(delta);
    }

    public Instant rescheduleWithDefaultPeriod() {
        return reschedule(period);
    }

    protected abstract String getTaskId();

    public void stopTask() {
        clock.removeTask(getTaskId());
    }

}
