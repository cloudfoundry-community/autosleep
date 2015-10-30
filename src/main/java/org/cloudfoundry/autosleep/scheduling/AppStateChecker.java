package org.cloudfoundry.autosleep.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class AppStateChecker implements Runnable {

    protected final UUID appUid;
    protected final String taskId;
    protected final Duration period;

    protected final CloudFoundryApiService remote;
    protected final Clock clock;

    public void start() {
        clock.scheduleTask(taskId, Duration.ofSeconds(0), this);
    }

    @Override
    public void run() {
        log.debug("Checking on app {} state, for taskId {}", appUid, taskId);
        ApplicationInfo applicationInfo = remote.getApplicationInfo(appUid);

        if (CloudApplication.AppState.STOPPED.equals(applicationInfo.getState())) {
            log.debug("App already stopped.");
            rescheduleWithDefaultPeriod();
        } else {
            //retrieve updated info
            LocalDateTime lastEvent = applicationInfo.getLastEvent();
            //TODO check if LocalDate issue between remote dates and app time
            LocalDateTime nextStartTime = lastEvent.plus(period);
            log.debug("last event:  {}", lastEvent.toString());

            if (nextStartTime.isBefore(LocalDateTime.now())) {
                log.debug("Inactivity detected, stopping application");
                remote.stopApplication(appUid);
                rescheduleWithDefaultPeriod();
            } else {
                //rescheduled itself
                Duration delta = Duration.between(LocalDateTime.now(), nextStartTime);
                reschedule(delta);
            }
        }
    }

    protected void reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(taskId, delta, this);
    }

    protected void rescheduleWithDefaultPeriod() {
        reschedule(period);
    }

    public void stop() {
        clock.stopTimer(taskId);
    }


}
