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
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

    @Autowired
    private CloudFoundryClient client;

    @Override
    public ApplicationActivity getApplicationActivity(UUID appUid) {
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
                log.debug("Building ApplicationInfo(lastEventTime={}, lastLogTime={}, state)",
                        lastEventTime, lastLogTime);
                return new ApplicationActivity(app.getMeta().getGuid(), app.getName(), app.getState(),
                        lastEventTime == null ? null : lastEventTime.toInstant(),
                        lastLogTime == null ? null : lastLogTime.toInstant());
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
            ApplicationInfo app = getApplicationInfo(appUid);
            if (app != null) {
                if (app.getState() != AppState.STOPPED) {
                    client.stopApplication(app.getName());
                } else {
                    log.debug("App {} already stopped", app.getName());
                }
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
    public List<UUID> listApplications(UUID spaceUuid, String excludeNamesExpression) {
        try {
            return client.getApplications().stream()
                    .filter(
                            cloudApplication -> (spaceUuid == null
                                    ||
                                    spaceUuid.equals(cloudApplication.getSpace().getMeta().getGuid()))
                                    &&
                                    (excludeNamesExpression == null
                                            ||
                                            cloudApplication.getName().matches(excludeNamesExpression))
                    )
                    .map(cloudApplication ->
                            cloudApplication.getMeta().getGuid())
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            log.error("error", r);
            return null;
        }
    }

}
