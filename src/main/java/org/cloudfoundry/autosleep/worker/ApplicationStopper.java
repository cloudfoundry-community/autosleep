package org.cloudfoundry.autosleep.worker;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.worker.scheduling.AbstractPeriodicTask;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.worker.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.worker.remote.EntityNotFoundException;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.util.LastDateComputer;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
class ApplicationStopper extends AbstractPeriodicTask {

    private final UUID appUid;

    private final String spaceEnrollerConfigId;

    private final String taskId;

    private final CloudFoundryApiService cloudFoundryApi;

    private final ApplicationRepository applicationRepository;

    private final ApplicationLocker applicationLocker;

    @Builder
    ApplicationStopper(Clock clock, Duration period, UUID appUid, String spaceEnrollerConfigId, String taskId,
                       CloudFoundryApiService cloudFoundryApi, ApplicationRepository applicationRepository,
                       ApplicationLocker applicationLocker) {
        super(clock, period);
        this.appUid = appUid;
        this.spaceEnrollerConfigId = spaceEnrollerConfigId;
        this.taskId = taskId;
        this.cloudFoundryApi = cloudFoundryApi;
        this.applicationRepository = applicationRepository;
        this.applicationLocker = applicationLocker;
    }


    @Override
    public void run() {
        applicationLocker.executeThreadSafe(this.appUid.toString(), () -> {
            ApplicationInfo applicationInfo = applicationRepository.findOne(appUid.toString());
            if (applicationInfo == null) {
                handleApplicationNotFound();
            } else {
                if (applicationInfo.getEnrollmentState().isEnrolledByService(spaceEnrollerConfigId)) {
                    handleApplicationEnrolled(applicationInfo);
                } else {
                    handleApplicationBlackListed(applicationInfo);
                }
            }
        });
    }

    protected void handleApplicationNotFound() {
        log.debug("Application unknown (must have unbound). Cancelling task.");
        stopTask();
    }


    protected void handleApplicationBlackListed(ApplicationInfo applicationInfo) {
        log.debug("Known application, but ignored (blacklisted). Cancelling task.");
        stopTask();
        applicationInfo.clearCheckInformation();
        applicationRepository.save(applicationInfo);
    }


    protected void handleApplicationEnrolled(ApplicationInfo applicationInfo) {
        Duration rescheduleDelta = null;
        try {
            ApplicationActivity applicationActivity = cloudFoundryApi.getApplicationActivity(appUid);
            log.debug("Checking on app {} state", appUid);
            applicationInfo.updateDiagnosticInfo(applicationActivity.getState(), applicationActivity.getLastLog(),
                    applicationActivity.getLastEvent(), applicationActivity.getApplication().getName());
            if (applicationActivity.getState() == CloudApplication.AppState.STOPPED) {
                log.debug("App already stopped.");
            } else {
                rescheduleDelta = checkActiveApplication(applicationInfo, applicationActivity);
            }
        } catch (EntityNotFoundException c) {
            log.error("application not found. should not appear cause should not be in repository anymore", c);
        } catch (CloudFoundryException c) {
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
        return taskId;
    }
}
