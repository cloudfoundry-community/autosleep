/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.worker;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationActivity;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.access.dao.model.ProxyMapEntry;
import org.cloudfoundry.autosleep.access.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.ProxyMapEntryRepository;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.util.LastDateComputer;
import org.cloudfoundry.autosleep.worker.scheduling.AbstractPeriodicTask;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
class ApplicationStopper extends AbstractPeriodicTask {

    private final String appUid;

    private final ApplicationLocker applicationLocker;

    private final ApplicationRepository applicationRepository;

    private final String bindingId;

    private final CloudFoundryApiService cloudFoundryApi;

    private final Boolean ignoreRouteBindingError;

    private final ProxyMapEntryRepository proxyMap;

    private final String spaceEnrollerConfigId;

    @Builder
    ApplicationStopper(Clock clock,
                       Duration period,
                       String appUid,
                       String spaceEnrollerConfigId,
                       String bindingId,
                       CloudFoundryApiService cloudFoundryApi,
                       ApplicationRepository applicationRepository,
                       ApplicationLocker applicationLocker,
                       boolean ignoreRouteBindingError,
                       ProxyMapEntryRepository proxyMap) {
        super(clock, period);
        this.appUid = appUid;
        this.spaceEnrollerConfigId = spaceEnrollerConfigId;
        this.bindingId = bindingId;
        this.cloudFoundryApi = cloudFoundryApi;
        this.applicationRepository = applicationRepository;
        this.applicationLocker = applicationLocker;
        this.ignoreRouteBindingError = ignoreRouteBindingError;
        this.proxyMap = proxyMap;
    }

    private Duration checkActiveApplication(ApplicationInfo applicationInfo, ApplicationActivity applicationActivity)
            throws CloudFoundryException {
        //retrieve updated info
        Duration delta = null;
        Instant lastEvent = LastDateComputer.computeLastDate(
                applicationActivity.getLastLog(),
                applicationActivity.getLastEvent());
        if (lastEvent != null) {
            Instant nextIdleTime = lastEvent.plus(getPeriod());
            log.debug("last event:  {}", lastEvent.toString());

            if (nextIdleTime.isBefore(Instant.now())) {
                putApplicationToSleep(applicationInfo, applicationActivity);
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

            applicationInfo.updateDiagnosticInfo(
                    applicationActivity.getLastLog(),
                    applicationActivity.getLastEvent(),
                    applicationActivity.getApplication().getName(),
                    applicationActivity.getState());
            if (CloudFoundryAppState.STOPPED.equals(applicationActivity.getState())) {
                log.debug("App already stopped.");
            } else {
                rescheduleDelta = checkActiveApplication(applicationInfo, applicationActivity);
            }
        } catch (CloudFoundryException c) {
            log.error("error while requesting cloudfoundry api", c);
        } catch (Throwable t) {
            log.error("unsuspected error", t);
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

    protected void handleApplicationNotFound() {
        log.debug("Application unknown (must have unbound). Cancelling task.");
        stopTask();
    }

    private void putApplicationToSleep(ApplicationInfo applicationInfo, ApplicationActivity applicationActivity) throws
            CloudFoundryException {
        log.info("Stopping app [{} / {}], last event: {}, last log: {}",
                applicationActivity.getApplication().getName(), appUid,
                applicationActivity.getLastEvent(), applicationActivity.getLastLog());

        //retrieve all routes for this app
        List<String> routeIds = cloudFoundryApi.listApplicationRoutes(appUid);
        /*TODO uncomment whenever route services handle stopped apps
        try {
            cloudFoundryApi.bindRoutes(spaceEnrollerConfigId, routeIds);
        } catch (CloudFoundryException c) {
            if (this.ignoreRouteBindingError == null || !this.ignoreRouteBindingError) {
                throw c;
            } else {
                log.debug("Skip route binding error or {} on application {}.", spaceEnrollerConfigId, appUid);
            }
        }*/

        routeIds.forEach(id -> {
            try {
                String host = cloudFoundryApi.getHost(id);
                log.debug("Got host {}", host);
                proxyMap.save(new ProxyMapEntry(host, appUid, false));
            } catch (CloudFoundryException e) {
                log.error("Couldn't get host corresponding to a route ", e);
            }
        });

        cloudFoundryApi.stopApplication(appUid);
        applicationInfo.markAsPutToSleep();
    }

    @Override
    public void run() {
        applicationLocker.executeThreadSafe(this.appUid,
                () -> {
                    ApplicationInfo applicationInfo = applicationRepository.findOne(appUid);
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

}
