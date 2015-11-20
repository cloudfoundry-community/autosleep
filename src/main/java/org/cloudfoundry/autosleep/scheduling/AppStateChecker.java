package org.cloudfoundry.autosleep.scheduling;

import lombok.Builder;
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

@Slf4j
public class AppStateChecker extends AbstractPeriodicTask {

    private final UUID appUid;

    private final String bindingId;

    private final CloudFoundryApiService cloudFoundryApi;

    private final ApplicationRepository applicationRepository;

    @Builder
    AppStateChecker(Clock clock, Duration period, UUID appUid, String bindingId,
                    CloudFoundryApiService cloudFoundryApi, ApplicationRepository applicationRepository) {
        super(clock, period);
        this.appUid = appUid;
        this.bindingId = bindingId;
        this.cloudFoundryApi = cloudFoundryApi;
        this.applicationRepository = applicationRepository;
    }


    @Override
    public void run() {
        ApplicationInfo applicationInfo = applicationRepository.findOne(appUid.toString());
        if (applicationInfo == null) {
            log.error("ApplicationInfo is null, this should never happen!");
            stopTask();
        } else {
            switch (applicationInfo.getStateMachine().getState()) {
                case IGNORED:
                    log.debug("App has been unbound. Cancelling task.");
                    stopTask();
                    applicationInfo.clearCheckInformation();
                    applicationRepository.save(applicationInfo);
                    break;
                case MONITORED:
                    ApplicationActivity applicationActivity = cloudFoundryApi.getApplicationActivity(appUid);
                    Instant nextCheckTime;
                    if (applicationActivity != null) {
                        log.debug("Checking on app {} state, for bindingId {}", appUid, bindingId);
                        applicationInfo.updateRemoteInfo(applicationActivity);
                        if (applicationActivity.getState() == CloudApplication.AppState.STOPPED) {
                            log.debug("App already stopped.");
                            nextCheckTime = rescheduleWithDefaultPeriod();
                        } else {
                            //retrieve updated info
                            Instant lastEvent = LastDateComputer.computeLastDate(applicationActivity
                                            .getLastLog(),
                                    applicationActivity.getLastEvent());
                            if (lastEvent != null) {
                                Instant nextIdleTime = lastEvent.plus(getPeriod());
                                log.debug("last event:  {}", lastEvent.toString());

                                if (nextIdleTime.isBefore(Instant.now())) {
                                    log.info("Stopping app [{} / {}], last event: {}, last log: {}",
                                            applicationActivity.getApplication().getName(), appUid,
                                            applicationActivity.getLastEvent(), applicationActivity.getLastLog());
                                    cloudFoundryApi.stopApplication(appUid);
                                    applicationInfo.markAsPutToSleep();
                                    nextCheckTime = rescheduleWithDefaultPeriod();
                                } else {
                                    //rescheduled itself
                                    Duration delta = Duration.between(Instant.now(), nextIdleTime);
                                    nextCheckTime = reschedule(delta);
                                }
                            } else {
                                log.error("cannot find last event");
                                nextCheckTime = rescheduleWithDefaultPeriod();
                            }
                        }
                    } else {
                        log.debug("failed to retrieve application activity informations");
                        nextCheckTime = rescheduleWithDefaultPeriod();
                    }
                    applicationInfo.markAsChecked(nextCheckTime);
                    applicationRepository.save(applicationInfo);
                    break;

                default:
                    log.error("unkown state");
            }
        }

    }

    @Override
    protected String getTaskId() {
        return bindingId;
    }
}
