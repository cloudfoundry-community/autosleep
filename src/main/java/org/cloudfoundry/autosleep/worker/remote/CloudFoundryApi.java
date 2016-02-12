package org.cloudfoundry.autosleep.worker.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationActivity;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;
import org.cloudfoundry.client.logging.LogMessage;
import org.cloudfoundry.client.logging.RecentLogsRequest;
import org.cloudfoundry.client.spring.SpringCloudFoundryClient;
import org.cloudfoundry.client.spring.SpringLoggingClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.EventResource;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

    @Autowired
    private SpringCloudFoundryClient cfClient;

    private SpringLoggingClient loggregatorClient;

    @Override
    public void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId) throws
            CloudFoundryException {
        log.debug("bindServiceInstance");
        try {
            cfClient.serviceBindings().create(
                    CreateServiceBindingRequest
                            .builder()
                            .applicationId(application.getGuid())
                            .serviceInstanceId(serviceInstanceId).build()).get(
                    Config.CF_API_TIMEOUT_IN_S, TimeUnit
                            .SECONDS);
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public void bindServiceInstance(List<ApplicationIdentity> applications, String serviceInstanceId) throws
            CloudFoundryException {
        log.debug("bindServiceInstance list");
        for (ApplicationIdentity application : applications) {
            bindServiceInstance(application, serviceInstanceId);
        }
    }

    private ApplicationInfo.DiagnosticInfo.ApplicationEvent buildAppEvent(EventEntity cfEvent) {
        ApplicationInfo.DiagnosticInfo.ApplicationEvent applicationEvent =
                new ApplicationInfo.DiagnosticInfo.ApplicationEvent(cfEvent.getType());
        applicationEvent.setActor(cfEvent.getActor());
        applicationEvent.setActee(cfEvent.getActee());
        applicationEvent.setTimestamp(Instant.parse(cfEvent.getTimestamp()));
        applicationEvent.setType(cfEvent.getType());
        return applicationEvent;
    }

    private ApplicationInfo.DiagnosticInfo.ApplicationLog buildAppLog(LogMessage cfLog) {
        return cfLog == null ? null : new ApplicationInfo.DiagnosticInfo.ApplicationLog(
                cfLog.getMessage(),
                cfLog.getTimestamp().toInstant(),
                cfLog.getMessageType().toString(),
                cfLog.getSourceName(),
                cfLog.getSourceId());
    }

    private void changeApplicationState(String applicationUuid, String targetState) throws CloudFoundryException {
        log.debug("changeApplicationState to {}",targetState);
        try {
            Mono<GetApplicationResponse> publisher = this.cfClient
                    .applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationUuid).build());
            GetApplicationResponse response = publisher.get(Config.CF_API_TIMEOUT_IN_S,TimeUnit.SECONDS);

            if (!targetState.equals(response.getEntity().getState())) {
                cfClient.applicationsV2().update(
                        UpdateApplicationRequest.builder().applicationId(applicationUuid)
                                .state(targetState).build()).get(Config.CF_API_TIMEOUT_IN_S,TimeUnit.SECONDS);
            } else {
                log.warn("application {} already in state {}, nothing to do", applicationUuid, targetState);
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException {
        log.debug("Getting applicationActivity {}", appUid);
        try {
            ApplicationEntity app = this.cfClient.applicationsV2().get(GetApplicationRequest.builder()
                    .applicationId(appUid).build()).get(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS).getEntity();
            log.debug("Getting application info for app {}", app.getName());
            log.debug("Building ApplicationInfo(state) {}", app.getState());

            // get most recent app event
            Mono<ListEventsResponse> listEventsResponse = this.cfClient.events().list(
                    ListEventsRequest.builder()
                            .actee(appUid).build());
            List<EventResource> events = listEventsResponse.get(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS)
                    .getResources();

            //get most recent log
            if (loggregatorClient == null) {
                loggregatorClient = SpringLoggingClient.builder().cloudFoundryClient(this.cfClient).build();
            }

            Publisher<LogMessage> logPublisher = loggregatorClient.recent(RecentLogsRequest.builder()
                    .applicationId(appUid)
                    .build());

            LogMessage mostRecentMessageLog;
            try {
                mostRecentMessageLog = getMostRecentLog(logPublisher);
            } catch (Throwable throwable) {
                throw new CloudFoundryException(throwable);
            }

            return new ApplicationActivity(
                    new ApplicationIdentity(
                            appUid,
                            app.getName()),
                    app.getState(),
                    events.size() == 0 ? null : buildAppEvent(events.get(events.size() - 1).getEntity()),
                    buildAppLog(mostRecentMessageLog));
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }

    }

    private LogMessage getMostRecentLog(Publisher<LogMessage> publisher) throws
            CloudFoundryException {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);
        final AtomicReference<LogMessage> mostRecentMessageLog = new AtomicReference<>(null);

        Subscriber<LogMessage> recentLogFinderSubscriber = new Subscriber<LogMessage>() {

            Instant mostRecentInstant;

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                latch.countDown();
            }

            @Override
            public void onNext(LogMessage message) {
                Instant msgInstant = message.getTimestamp().toInstant();
                if (mostRecentInstant == null || mostRecentInstant.isBefore(msgInstant)) {
                    mostRecentInstant = msgInstant;
                    mostRecentMessageLog.set(message);
                }
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
        };

        publisher.subscribe(recentLogFinderSubscriber);
        try {
            if (!latch.await(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Subscriber timed out");
            } else if (error.get() != null) {
                throw new CloudFoundryException(error.get());
            } else {
                return mostRecentMessageLog.get();
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public List<ApplicationIdentity> listApplications(String spaceUuid, Pattern excludeNames) throws
            CloudFoundryException {
        log.debug("listApplications");
        try {
            ListApplicationsResponse response = this.cfClient
                    .applicationsV2()
                    .list(ListApplicationsRequest.builder().spaceId(spaceUuid).build()).get(Config
                            .CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS);
            return response.getResources().stream().filter(
                    cloudApplication -> (
                            spaceUuid == null
                                    || spaceUuid.equals(cloudApplication.getEntity().getSpaceId()))
                            && (
                            excludeNames == null
                                    || !excludeNames.matcher(cloudApplication.getEntity().getName()).matches()))
                    .map(
                            cloudApplication -> new ApplicationIdentity(
                                    cloudApplication.getMetadata().getId(),
                                    cloudApplication.getEntity().getName()))
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public void startApplication(String applicationUuid) throws CloudFoundryException {
        log.debug("startApplication");
        changeApplicationState(applicationUuid, CloudFoundryAppState.STARTED);
    }

    @Override
    public void stopApplication(String applicationUuid) throws CloudFoundryException {
        log.debug("stopApplication");
        changeApplicationState(applicationUuid, CloudFoundryAppState.STOPPED);
    }
}
