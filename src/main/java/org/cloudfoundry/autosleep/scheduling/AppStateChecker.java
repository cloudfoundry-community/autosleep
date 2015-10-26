package org.cloudfoundry.autosleep.scheduling;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApi;

import java.time.Duration;
import java.time.LocalDateTime;

@AllArgsConstructor
@Slf4j
public class AppStateChecker implements Runnable {

    protected final String appGuid;
    protected final String taskId;
    protected final Duration period;

    protected final CloudFoundryApi remote;
    protected final Clock clock;

    public void start() {
        clock.scheduleTask(taskId, Duration.ofSeconds(0), this);
    }

    @Override
    public void run() {
        log.debug("Checking on app {} state, for taskId {}", appGuid, taskId);
        //retrieve updated info
        LocalDateTime lastEvent = remote.getApplicationInfo(appGuid).getLastEvent();

        //TODO check if LocalDate issue between remote dates and app time
        LocalDateTime nextStartTime = lastEvent.plus(period);
        log.debug("last event:  {}", lastEvent.toString());

        if (nextStartTime.isBefore(LocalDateTime.now())) {
            log.debug("Inactivity detected");
            remote.stopApplication(appGuid);
        } else {
            //rescheduled itself
            Duration delta = Duration.between(LocalDateTime.now(), nextStartTime);
            log.debug("Rescheduling for {}", delta.toString());
            clock.scheduleTask(taskId, delta, this);
        }

    }

    public void stop() {
        clock.stopTimer(taskId);
    }



}
