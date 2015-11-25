package org.cloudfoundry.autosleep.scheduling;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException;
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
            handleApplicationNotFound();
        } else {
            switch (applicationInfo.getStateMachine().getState()) {
                case IGNORED:
                    handleApplicationIgnored(applicationInfo);
                    break;
                case MONITORED:
                    handleApplicationMonitored(applicationInfo);
                    break;

                default:
                    log.error("unknown state");
            }
        }
    }

    private void handleApplicationNotFound() {
        log.error("ApplicationInfo is null, this should never happen!");
        stopTask();
    }


    private void handleApplicationIgnored(ApplicationInfo applicationInfo) {
        log.debug("App has been unbound. Cancelling task.");
        stopTask();
        applicationInfo.clearCheckInformation();
        applicationRepository.save(applicationInfo);
    }


    private void handleApplicationMonitored(ApplicationInfo applicationInfo) {
        Duration rescheduleDelta = null;
        try {
            ApplicationActivity applicationActivity = cloudFoundryApi.getApplicationActivity(appUid);
            log.debug("Checking on app {} state, for bindingId {}", appUid, bindingId);
            applicationInfo.updateRemoteInfo(applicationActivity);
            if (applicationActivity.getState() == CloudApplication.AppState.STOPPED) {
                log.debug("App already stopped.");
            } else {
                rescheduleDelta = checkActiveApplication(applicationInfo, applicationActivity);
            }
        } catch (EntityNotFoundException c) {
            log.error("application not found. should not appear cause should not be in repository anymore", c);
        }  catch (CloudFoundryException c) {
            log.error("error while requesting remote api", c);
        } finally {
            Instant nextCheckTime;
            if (rescheduleDelta == null) {
                nextCheckTime = rescheduleWithDefaultPeriod();
            } else {
                nextCheckTime = reschedule(rescheduleDelta);
            }
            applicationInfo.markAsChecked(nextCheckTime);
            applicationRepository.save(applicationInfo);
        }

    }


    private Duration checkActiveApplication(ApplicationInfo applicationInfo, ApplicationActivity applicationActivity)
            throws EntityNotFoundException, CloudFoundryException {
        //retrieve updated info
        Duration delta = null;
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
            } else {
                //rescheduled itself
                delta = Duration.between(Instant.now(), nextIdleTime);
            }
        } else {
            log.error("cannot find last event");
        }
        return delta;
    }

    @Override
    protected String getTaskId() {
        return bindingId;
    }
}
