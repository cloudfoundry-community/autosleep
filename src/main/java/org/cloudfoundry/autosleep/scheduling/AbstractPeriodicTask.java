package org.cloudfoundry.autosleep.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@AllArgsConstructor
abstract class AbstractPeriodicTask implements Runnable {

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

    protected Instant reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(getTaskId(), delta, this);
        return Instant.now().plus(delta);
    }

    protected Instant rescheduleWithDefaultPeriod() {
        return reschedule(period);
    }

    protected abstract String getTaskId();

    protected void stopTask() {
        clock.removeTask(getTaskId());
    }

}
