package org.cloudfoundry.autosleep.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.util.LastDateComputer;
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

    private ApplicationRepository appRepository;

    public void start() {
        clock.scheduleTask(bindingId, Duration.ofSeconds(0), this);
    }

    @Override
    public void run() {
        ApplicationInfo applicationInfo = appRepository.findOne(appUid.toString());
        if (applicationInfo == null) {
            log.error("ApplicationInfo is null, this should never happen!");
            clock.removeTask(bindingId);
        } else {
            switch (applicationInfo.getStateMachine().getState()) {
                case IGNORED:
                    log.debug("App has been unbound. Cancelling task.");
                    clock.removeTask(bindingId);
                    applicationInfo.setCheckTimes(Instant.now(),null);
                    appRepository.save(applicationInfo);
                    break;
                case MONITORED:
                    ApplicationActivity applicationActivity = remote.getApplicationActivity(appUid);
                    Instant nextCheckTime = null;
                    if (applicationActivity != null) {
                        log.debug("Checking on app {} state, for bindingId {}", appUid, bindingId);
                        if (applicationActivity.getState() == CloudApplication.AppState.STOPPED) {
                            log.debug("App already stopped.");
                            nextCheckTime = rescheduleWithDefaultPeriod();
                        } else {
                            //retrieve updated info
                            Instant lastEvent = LastDateComputer.computeLastDate(applicationActivity
                                            .getLastLog(),
                                    applicationActivity.getLastEvent());
                            if (lastEvent != null) {
                                Instant nextIdleTime = lastEvent.plus(period);
                                log.debug("last event:  {}", lastEvent.toString());

                                if (nextIdleTime.isBefore(Instant.now())) {
                                    log.info("Stopping app [{} / {}], last event: {}, last log: {}",
                                            applicationActivity.getName(), appUid,
                                            applicationActivity.getLastEvent(), applicationActivity.getLastLog());
                                    remote.stopApplication(appUid);
                                    nextCheckTime = rescheduleWithDefaultPeriod();
                                } else {
                                    //rescheduled itself
                                    Duration delta = Duration.between(Instant.now(), nextIdleTime);
                                    nextCheckTime = reschedule(delta);
                                }
                            } else {
                                log.error("cannot find last event");
                                nextCheckTime = reschedule(period);
                            }
                        }
                    } else {
                        log.debug("failed to retrieve application activity informations");
                        nextCheckTime = rescheduleWithDefaultPeriod();
                    }
                    applicationInfo.setCheckTimes(Instant.now(),nextCheckTime);
                    appRepository.save(applicationInfo);
                    break;

                default:
                    log.error("unkown state");
            }
        }

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
