package org.cloudfoundry.autosleep.worker.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.worker.remote.EntityNotFoundException.EntityType;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationActivity;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

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
    public ApplicationActivity getApplicationActivity(UUID appUid)
            throws EntityNotFoundException, CloudFoundryException {
        try {
            CloudApplication app = getApplication(appUid);
            log.debug("Getting application info for app {}", app.getName());
            List<ApplicationLog> lastLogs = client.getRecentLogs(app.getName());
            List<CloudEvent> events = client.getApplicationEvents(app.getName());
            log.debug("Building ApplicationInfo(state)", app.getState());

            return new ApplicationActivity(new ApplicationIdentity(app.getMeta().getGuid().toString(),
                    app.getName()),
                    app.getState(),
                    events.size() == 0 ? null : buildAppEvent(events.get(events.size() - 1)),
                    lastLogs.size() == 0  ? null : buildAppLog(lastLogs.get(lastLogs.size() - 1)));

        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }


    @Override
    public void stopApplication(UUID applicationUuid) throws EntityNotFoundException, CloudFoundryException {
        try {
            CloudApplication app = getApplication(applicationUuid);
            if (app.getState() != AppState.STOPPED) {
                client.stopApplication(applicationUuid);
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public void startApplication(UUID applicationUuid) throws EntityNotFoundException, CloudFoundryException {
        try {
            CloudApplication app = getApplication(applicationUuid);
            if (app.getState() != AppState.STARTED) {
                log.info("Starting app {} - {}", applicationUuid, app.getName());
                client.startApplication(applicationUuid);
            } else {
                log.debug("App {} already started", app.getName());
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public List<ApplicationIdentity> listApplications(UUID spaceUuid, Pattern excludeNames)
            throws CloudFoundryException {
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
                            new ApplicationIdentity(cloudApplication.getMeta().getGuid().toString(),
                                    cloudApplication.getName()))
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId)
            throws EntityNotFoundException, CloudFoundryException {
        CloudService service = getService(serviceInstanceId);
        log.debug("service {} - {} found", service.getMeta().getGuid(),
                service.getName());
        bindServiceInstanceToApplication(application, service);
    }

    @Override
    public void bindServiceInstance(List<ApplicationIdentity> applications, String serviceInstanceId)
            throws EntityNotFoundException, CloudFoundryException {
        CloudService service = getService(serviceInstanceId);
        log.debug("service {} - {} found", service.getMeta().getGuid(), service.getName());
        for (ApplicationIdentity application : applications) {
            bindServiceInstanceToApplication(application, service);
        }
    }


    private void bindServiceInstanceToApplication(ApplicationIdentity application, CloudService cloudService)
            throws CloudFoundryException {
        try {
            log.debug("binding app {} - {} to service {}", application.getGuid(), application.getName(),
                    cloudService.getName());
            client.bindService(UUID.fromString(application.getGuid()), cloudService.getMeta().getGuid());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }


    private CloudApplication getApplication(UUID applicationUuid)
            throws EntityNotFoundException, CloudFoundryException {
        try {
            return client.getApplication(applicationUuid);
        } catch (HttpStatusCodeException r) {
            if (HttpStatus.NOT_FOUND.equals(r.getStatusCode())) {
                throw new EntityNotFoundException(EntityType.application, applicationUuid.toString(), r);
            } else {
                throw new CloudFoundryException(r);
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }


    private CloudService getService(String serviceInstanceId) throws CloudFoundryException, EntityNotFoundException {
        try {
            Optional<CloudService> cloudService = client.getServices().stream()
                    .filter(service -> service.getMeta().getGuid().toString().equals(serviceInstanceId))
                    .findFirst();
            if (cloudService.isPresent()) {
                return cloudService.get();
            } else {
                throw new EntityNotFoundException(EntityType.service, serviceInstanceId);
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    private ApplicationInfo.DiagnosticInfo.ApplicationEvent buildAppEvent(CloudEvent cfEvent) {
        ApplicationInfo.DiagnosticInfo.ApplicationEvent applicationEvent =
                new ApplicationInfo.DiagnosticInfo.ApplicationEvent(cfEvent.getName());
        applicationEvent.setActor(cfEvent.getActor());
        applicationEvent.setActee(cfEvent.getActee());
        applicationEvent.setTimestamp(cfEvent.getTimestamp().toInstant());
        applicationEvent.setType(cfEvent.getType());
        return applicationEvent;
    }

    private ApplicationInfo.DiagnosticInfo.ApplicationLog buildAppLog(ApplicationLog cfLog) {
        return new ApplicationInfo.DiagnosticInfo.ApplicationLog(
                cfLog.getMessage(),
                cfLog.getTimestamp().toInstant(),
                cfLog.getMessageType().toString(),
                cfLog.getSourceName(),
                cfLog.getSourceId());
    }
}
