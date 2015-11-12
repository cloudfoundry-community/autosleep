package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

    @Autowired
    private CloudFoundryClient client;

    @Override
    public ApplicationInfo getApplicationInfo(UUID appUid) {
        try {
            CloudApplication app = client.getApplication(appUid);
            if (app != null) {
                log.debug("Getting application info for app {}", app.getName());
                List<ApplicationLog> lastLogs = client.getRecentLogs(app.getName());
                List<CloudEvent> events = client.getApplicationEvents(app.getName());
                Date lastLogTime = null;
                Date lastEventTime = null;

                if (lastLogs.size() > 0) {
                    lastLogTime = lastLogs.get(lastLogs.size() - 1).getTimestamp();
                }
                if (events.size() > 0) {
                    lastEventTime = events.get(events.size() - 1).getTimestamp();
                } else {
                    log.debug("events.size() = 0");
                }

                //conversion to LocalDateTime

                log.debug("Building ApplicationInfo(lastEventTime={}, lastLogTime={}, state)",
                        lastEventTime, lastLogTime);
                return new ApplicationInfo(lastEventTime == null ? null : lastEventTime.toInstant(),
                        lastLogTime == null ? null : lastLogTime.toInstant(),
                        app.getState());
            } else {
                log.error("No app found for UID {}", appUid);
                return null;
            }
        } catch (RuntimeException t) {
            log.error("error", t);
            return null;
        }
    }


    @Override
    public void stopApplication(UUID appUid) {
        try {
            CloudApplication app = client.getApplication(appUid);
            if (app != null) {
                if (app.getState() != AppState.STOPPED) {
                    ApplicationInfo appInfo = getApplicationInfo(appUid);
                    log.info("Stopping app [{} / {}], last event: {}, last log: {}", app.getName(), appUid,
                            appInfo.getLastEvent(), appInfo.getLastLog());
                    client.stopApplication(app.getName());
                } else {
                    log.debug("App {} already stopped", app.getName());
                }
            } else {
                log.error("No app found for UID {}", appUid);
            }
        } catch (RuntimeException r) {
            log.error("error", r);
        }
    }

    @Override
    public void startApplication(UUID appUid) {
        try {
            CloudApplication app = client.getApplication(appUid);
            if (app != null) {
                if (app.getState() != AppState.STARTED) {
                    log.info("Starting app {} - {}", appUid, app.getName());
                    client.startApplication(app.getName());
                } else {
                    log.debug("App {} already started", app.getName());
                }

            } else {
                log.error("No app found for UID {}", appUid);
            }
        } catch (RuntimeException r) {
            log.error("error", r);
        }
    }

    @Override
    public List<String> getApplicationsNames() {
        return null;
    }


}
