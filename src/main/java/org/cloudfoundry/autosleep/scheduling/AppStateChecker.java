package org.cloudfoundry.autosleep.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
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

    private ApplicationRepository appRepository;

    public void start() {
        clock.scheduleTask(bindingId, Duration.ofSeconds(0), this);
    }

    @Override
    public void run() {
        ApplicationInfo applicationInfo = remote.getApplicationInfo(appUid);
        Instant nextCheckTime = null;

        if (bindingRepository.findOne(bindingId) != null) {
            log.debug("Checking on app {} state, for bindingId {}", appUid, bindingId);


            if (applicationInfo.getState() == CloudApplication.AppState.STOPPED) {
                log.debug("App already stopped.");
                nextCheckTime = rescheduleWithDefaultPeriod();
            } else {
                //retrieve updated info
                Instant lastEvent = applicationInfo.computeLastDate();
                Instant nextIdleTime = lastEvent.plus(period);
                log.debug("last event:  {}", lastEvent.toString());

                if (nextIdleTime.isBefore(Instant.now())) {
                    log.debug("Inactivity detected, stopping application");
                    remote.stopApplication(appUid);
                    nextCheckTime = rescheduleWithDefaultPeriod();
                } else {
                    //rescheduled itself
                    Duration delta = Duration.between(Instant.now(), nextIdleTime);
                    nextCheckTime = reschedule(delta);
                }
            }
        } else {
            log.debug("binding no longer exist. cancelling task");
            clock.removeTask(bindingId);
        }

        applicationInfo.setNextCheck(nextCheckTime);
        appRepository.save(applicationInfo);
    }

    protected Instant reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(bindingId, delta, this);
        return Instant.now().plus(delta);
    }

    protected Instant rescheduleWithDefaultPeriod() {
        return reschedule(period);
    }

}
