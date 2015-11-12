package org.cloudfoundry.autosleep.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class AppStateChecker implements Runnable {

    private final UUID appUid;

    private final String bindingId;

    private final Duration period;

    private final CloudFoundryApiService remote;

    private final Clock clock;

    private BindingRepository bindingRepository;

    public void start() {
        clock.scheduleTask(bindingId, Duration.ofSeconds(0), this);
    }

    @Override
    public void run() {
        if (bindingRepository.findOne(bindingId) != null) {
            log.debug("Checking on app {} state, for bindingId {}", appUid, bindingId);
            ApplicationInfo applicationInfo = remote.getApplicationInfo(appUid);

            if (applicationInfo.getState() == CloudApplication.AppState.STOPPED) {
                log.debug("App already stopped.");
                rescheduleWithDefaultPeriod();
            } else {
                //retrieve updated info
                Instant lastEvent = applicationInfo.getLastActionDate();
                //TODO check if Instant issue between remote dates and app time
                Instant nextIdleTime = lastEvent.plus(period);
                log.debug("last event:  {}", lastEvent.toString());

                if (nextIdleTime.isBefore(Instant.now())) {
                    log.debug("Inactivity detected, stopping application");
                    remote.stopApplication(appUid);
                    rescheduleWithDefaultPeriod();
                } else {
                    //rescheduled itself
                    Duration delta = Duration.between(Instant.now(), nextIdleTime);
                    reschedule(delta);
                }
            }
        } else {
            log.debug("binding no longer exist. cancelling task");
            clock.removeTask(bindingId);
        }
    }

    protected void reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(bindingId, delta, this);
    }

    protected void rescheduleWithDefaultPeriod() {
        reschedule(period);
    }

}
