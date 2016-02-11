package org.cloudfoundry.autosleep.worker.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationActivity;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;
import org.cloudfoundry.client.LoggregatorClient;
import org.cloudfoundry.client.loggregator.LoggregatorMessage;
import org.cloudfoundry.client.spring.SpringCloudFoundryClient;
import org.cloudfoundry.client.spring.SpringLoggregatorClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

    @Autowired
    private SpringCloudFoundryClient cfClient;

    private LoggregatorClient loggregatorClient;

    @Override
    public void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId) throws
            CloudFoundryException {
        log.debug("bindServiceInstance");
        cfClient.serviceBindings().create(
                CreateServiceBindingRequest
                        .builder()
                        .applicationId(application.getGuid())
                        .serviceInstanceId(serviceInstanceId).build()).get(
                Config.CF_API_TIMEOUT_IN_S, TimeUnit
                        .SECONDS);
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

    private ApplicationInfo.DiagnosticInfo.ApplicationLog buildAppLog(LoggregatorMessage cfLog) {
        return cfLog == null ? null : new ApplicationInfo.DiagnosticInfo.ApplicationLog(
                cfLog.getMessage(),
                cfLog.getTimestamp().toInstant(),
                cfLog.getMessageType().toString(),
                cfLog.getSourceName(),
                cfLog.getSourceId());
    }

    private void changeApplicationState(String applicationUuid, String targetState) throws CloudFoundryException {
        log.debug("changeApplicationState");
        Mono<GetApplicationResponse> publisher = this.cfClient
                .applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationUuid).build());
        GetApplicationResponse response = publisher.get();

        if (targetState.equals(response.getEntity().getState())) {
            cfClient.applicationsV2().update(
                    UpdateApplicationRequest.builder().applicationId(applicationUuid)
                            .state(targetState).build());
        }
    }

    @Override
    public ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException {

     /*   ApplicationEntity app = this.cfClient.applicationsV2().get(GetApplicationRequest.builder()
                        .applicationId(appUid).build()).get(Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS).getEntity();
*/
        GetApplicationRequest getApplicationRequest = GetApplicationRequest.builder().applicationId(appUid).build();

        GetApplicationResponse getApplicationResponse = this.cfClient.applicationsV2().get(getApplicationRequest).get(
                Config.CF_API_TIMEOUT_IN_S, TimeUnit.SECONDS);
        ApplicationEntity app = getApplicationResponse.getEntity();

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
            loggregatorClient = SpringLoggregatorClient.builder().cloudFoundryClient(this.cfClient).build();
        }

       /* Publisher<LoggregatorMessage> logPublisher = loggregatorClient.recent(RecentLogsRequest.builder()
                .applicationId(appUid)
                .build());

        /*LoggregatorMessage mostRecentMessageLog;
       /* try {
            mostRecentMessageLog = getMostRecentLog(logPublisher);
        } catch (Throwable throwable) {
            throw new CloudFoundryException(throwable);
        }*/

        return new ApplicationActivity(
                new ApplicationIdentity(
                        appUid,
                        app.getName()),
                app.getState(),
                events.size() == 0 ? null : buildAppEvent(events.get(events.size() - 1).getEntity()),
                buildAppLog(null));

    }

    /*
        private LoggregatorMessage getMostRecentLog(Publisher<LoggregatorMessage> publisher) throws
        CloudFoundryException {

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> error = new AtomicReference<>(null);
            final AtomicReference<LoggregatorMessage> mostRecentMessageLog = new AtomicReference<>(null);

            Subscriber<LoggregatorMessage> recentLogFinderSubscriber = new Subscriber<LoggregatorMessage>() {

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
                public void onNext(LoggregatorMessage message) {
                    log.debug("the timestamp says: " + message.getTimestamp());
                    log.debug("the instant says: " + message.getTimestamp().toInstant());
                    Instant msgInstant = message.getTimestamp().toInstant();
                    if (mostRecentInstant.isBefore(msgInstant)) {
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
                if (!latch.await(10, TimeUnit.SECONDS)) {
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
    */
    @Override
    public List<ApplicationIdentity> listApplications(String spaceUuid, Pattern excludeNames) throws
            CloudFoundryException {
        log.debug("listApplications");
        Mono<ListApplicationsResponse> publisher = this.cfClient
                .applicationsV2()
                .list(ListApplicationsRequest.builder().spaceId(spaceUuid).build());

        log.debug("publisher content {}",publisher);
        ListApplicationsResponse response = publisher.get(Config.CF_API_TIMEOUT_IN_S,TimeUnit.SECONDS);
        log.debug("reponse {}", response);
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
