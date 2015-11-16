package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
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
                return new ApplicationActivity(new ApplicationIdentity(app.getMeta().getGuid(), app.getName()),
                        app.getState(),
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
    public void stopApplication(UUID applicationUuid) {
        try {
            CloudApplication app = client.getApplication(applicationUuid);
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
    public void startApplication(UUID applicationUuid) {
        try {
            CloudApplication app = client.getApplication(applicationUuid);
            if (app != null) {
                if (app.getState() != AppState.STARTED) {
                    log.info("Starting app {} - {}", applicationUuid, app.getName());
                    client.startApplication(app.getName());
                } else {
                    log.debug("App {} already started", app.getName());
                }

            } else {
                log.error("No app found for UID {}", applicationUuid);
            }
        } catch (RuntimeException r) {
            log.error("error", r);
        }
    }

    @Override
    public List<ApplicationIdentity> listApplications(UUID spaceUuid, Pattern excludeNames) {
        try {
            return client.getApplications().stream()
                    .filter(
                            cloudApplication -> (spaceUuid == null
                                    ||
                                    spaceUuid.equals(cloudApplication.getSpace().getMeta().getGuid()))
                                    &&
                                    (excludeNames == null
                                            ||
                                            !excludeNames.matcher(cloudApplication.getName()).matches())
                    )
                    .map(cloudApplication ->
                            new ApplicationIdentity(cloudApplication.getMeta().getGuid(), cloudApplication.getName()))
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            log.error("error", r);
            return null;
        }
    }

    @Override
    public void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId) {
        try {
            Optional<CloudService> cloudService = client.getServices().stream()
                    .filter(service -> service.getMeta().getGuid().toString().equals(serviceInstanceId))
                    .findFirst();
            if (cloudService.isPresent()) {
                log.debug("service {} - {} found", cloudService.get().getMeta().getGuid(),
                        cloudService.get().getName());
                bindServiceInstanceToApplication(application, cloudService.get());
            } else {
                log.error("No service found for ID {}", serviceInstanceId);
            }
        } catch (RuntimeException r) {
            log.error("error", r);
        }
    }

    @Override
    public void bindServiceInstance(List<ApplicationIdentity> applications, String serviceInstanceId) {
        try {
            Optional<CloudService> cloudService = client.getServices().stream()
                    .filter(service -> service.getMeta().getGuid().toString().equals(serviceInstanceId))
                    .findFirst();
            if (cloudService.isPresent()) {
                log.debug("service {} - {} found", cloudService.get().getMeta().getGuid(),
                        cloudService.get().getName());
                applications.forEach(application -> bindServiceInstanceToApplication(application, cloudService.get()));
            } else {
                log.error("No service found for ID {}", serviceInstanceId);
            }
        } catch (RuntimeException r) {
            log.error("error", r);
        }
    }


    private void bindServiceInstanceToApplication(ApplicationIdentity application, CloudService cloudService) {
        try {
            log.debug("binding app {} - {} to service {}", application.getGuid(), application.getName(),
                    cloudService.getName());
            client.bindService(application.getName(), cloudService.getName());
        } catch (RuntimeException r) {
            log.error("error", r);
        }
    }
}
