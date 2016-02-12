package org.cloudfoundry.autosleep.worker.remote;

import lombok.Getter;
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

    @Autowired
    private SpringLoggingClient logClient;

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

    private ApplicationInfo.DiagnosticInfo.ApplicationEvent buildAppEvent(EventResource event) {
        if (event == null) {
            return null;
        } else {
            EventEntity cfEvent = event.getEntity();
            ApplicationInfo.DiagnosticInfo.ApplicationEvent applicationEvent =
                    new ApplicationInfo.DiagnosticInfo.ApplicationEvent(cfEvent.getType());
            applicationEvent.setActor(cfEvent.getActor());
            applicationEvent.setActee(cfEvent.getActee());
            applicationEvent.setTimestamp(Instant.parse(cfEvent.getTimestamp()));
            applicationEvent.setType(cfEvent.getType());
            return applicationEvent;
        }
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
        log.debug("changeApplicationState to {}", targetState);
        try {
            Mono<GetApplicationResponse> publisher = this.cfClient
                    .applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationUuid).build());
            GetApplicationResponse response = publisher.get(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS);

            if (!targetState.equals(response.getEntity().getState())) {
                cfClient.applicationsV2().update(
                        UpdateApplicationRequest.builder().applicationId(applicationUuid)
                                .state(targetState).build()).get(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS);
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

       /* if (logClient == null) {
            logClient = SpringLoggingClient.builder().cloudFoundryClient(this.cfClient).build();
        }*/

        //We need to call for appState, lastlogs and lastEvents
        final CountDownLatch latch = new CountDownLatch(3);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);
        final AtomicReference<LogMessage> lastLogReference = new AtomicReference<>(null);
        final AtomicReference<EventResource> lastEventReference = new AtomicReference<>(null);
        final AtomicReference<GetApplicationResponse> appReference = new AtomicReference<>(null);

        Mono<GetApplicationResponse> getAppPublisher =
                cfClient.applicationsV2().get(GetApplicationRequest.builder().applicationId(appUid).build());

        Subscriber<GetApplicationResponse> getAppSubscriber = new BaseSubscriber<GetApplicationResponse>(latch, error) {

            @Override
            public void onNext(GetApplicationResponse getApplicationResponse) {
                appReference.set(getApplicationResponse);
            }

        };

        Mono<ListEventsResponse> listEventPublisher = this.cfClient.events().list(ListEventsRequest.builder()
                .actee(appUid).build());
        Subscriber<ListEventsResponse> listEventSubscriber = new BaseSubscriber<ListEventsResponse>(latch, error) {

            @Override
            public void onNext(ListEventsResponse listEventsResponse) {
                lastEventReference.set(listEventsResponse.getResources().get(0));
            }

        };

        Publisher<LogMessage> lastLogPublisher = logClient.recent(RecentLogsRequest.builder()
                .applicationId(appUid)
                .build());

        Subscriber<LogMessage> lastLogSubscriber = new BaseSubscriber<LogMessage>(latch, error) {

            Instant mostRecentInstant;

            @Override
            public void onNext(LogMessage logMessage) {
                //logs are not ordered, must find the most recent
                Instant msgInstant = logMessage.getTimestamp().toInstant();
                if (mostRecentInstant == null || mostRecentInstant.isBefore(msgInstant)) {
                    mostRecentInstant = msgInstant;
                    lastLogReference.set(logMessage);
                }
            }
        };

        lastLogPublisher.subscribe(lastLogSubscriber);
        getAppPublisher.subscribe(getAppSubscriber);
        listEventPublisher.subscribe(listEventSubscriber);

        try {
            if (!latch.await(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Subscriber timed out");
            } else if (error.get() != null) {
                throw new CloudFoundryException(error.get());
            } else {
                ApplicationEntity app = appReference.get().getEntity();
                return new ApplicationActivity(
                        new ApplicationIdentity(
                                appUid,
                                app.getName()),
                        app.getState(),
                        buildAppEvent(lastEventReference.get()),
                        buildAppLog(lastLogReference.get()));

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

    @Getter
    public abstract class BaseSubscriber<T> implements Subscriber<T> {

        final AtomicReference<Throwable> error;

        final CountDownLatch latch;

        public BaseSubscriber(CountDownLatch latch, AtomicReference<Throwable> error) {
            this.latch = latch;
            this.error = error;
        }

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
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
    }

}
