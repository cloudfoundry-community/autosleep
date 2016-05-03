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

package org.cloudfoundry.autosleep.access.cloudfoundry;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationActivity;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationIdentity;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.domains.GetDomainRequest;
import org.cloudfoundry.client.v2.domains.GetDomainResponse;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.EventResource;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.routes.GetRouteRequest;
import org.cloudfoundry.client.v2.routes.GetRouteResponse;
import org.cloudfoundry.client.v2.routes.ListRouteApplicationsRequest;
import org.cloudfoundry.client.v2.routes.ListRouteApplicationsResponse;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceToRouteRequest;
import org.cloudfoundry.logging.LogMessage;
import org.cloudfoundry.logging.LoggingClient;
import org.cloudfoundry.logging.RecentLogsRequest;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

    private static class BaseSubscriber<T> implements Subscriber<T> {

        Consumer<Throwable> errorConsumer;

        CountDownLatch latch;

        Consumer<T> resultConsumer;

        public BaseSubscriber(CountDownLatch latch, Consumer<Throwable> errorConsumer, Consumer<T> resultConsumer) {
            this.latch = latch;
            this.resultConsumer = resultConsumer;
            this.errorConsumer = errorConsumer;
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }

        @Override
        public void onError(Throwable throwable) {
            if (errorConsumer != null) {
                errorConsumer.accept(throwable);
            }
            latch.countDown();
        }

        @Override
        public void onNext(T result) {
            if (resultConsumer != null) {
                resultConsumer.accept(result);
            }
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }
    }

    @Autowired
    private CloudFoundryClient cfClient;

    @Autowired
    private LoggingClient logClient;

    private <T, U> void bind(List<T> objectsToBind, Function<T, Mono<U>> caller)
            throws CloudFoundryException {
        log.debug("bind - {} objects", objectsToBind.size());
        final CountDownLatch latch = new CountDownLatch(objectsToBind.size());
        final AtomicReference<Throwable> errorEncountered = new AtomicReference<>(null);
        final Subscriber<U> subscriber
                = new BaseSubscriber<>(latch, errorEncountered::set, null);

        objectsToBind.forEach(objectToBind -> caller.apply(objectToBind).subscribe(subscriber));
        waitForResult(latch, errorEncountered, null);
    }

    @Override
    public void bindApplications(String serviceInstanceId, List<ApplicationIdentity> applications) throws
            CloudFoundryException {
        bind(applications,
                application -> cfClient.serviceBindings()
                        .create(
                                CreateServiceBindingRequest
                                        .builder()
                                        .applicationId(application.getGuid())
                                        .serviceInstanceId(serviceInstanceId)
                                        .build()));
    }

    public void bindRoutes(String serviceInstanceId, List<String> routeIds) throws CloudFoundryException {
        bind(routeIds,
                routeId -> cfClient.serviceInstances()
                        .bindToRoute(
                                BindServiceInstanceToRouteRequest.builder()
                                        .serviceInstanceId(serviceInstanceId)
                                        .routeId(routeId)
                                        .build()));
    }

    private ApplicationInfo.DiagnosticInfo.ApplicationEvent buildAppEvent(EventResource event) {
        if (event == null) {
            return null;
        } else {
            EventEntity cfEvent = event.getEntity();
            return ApplicationInfo.DiagnosticInfo.ApplicationEvent.builder()
                    .actee(cfEvent.getActee())
                    .actor(cfEvent.getActor())
                    .name(cfEvent.getType())
                    .timestamp(Instant.parse(cfEvent.getTimestamp()).toEpochMilli())
                    .type(cfEvent.getType())
                    .build();
        }
    }

    private ApplicationInfo.DiagnosticInfo.ApplicationLog buildAppLog(LogMessage cfLog) {
        return cfLog == null ? null : ApplicationInfo.DiagnosticInfo.ApplicationLog.builder()
                .message(cfLog.getMessage())
                .timestamp(cfLog.getTimestamp().getTime())
                .messageType(cfLog.getMessageType().toString())
                .sourceId(cfLog.getSourceId())
                .sourceName(cfLog.getSourceName())
                .build();
    }

    private boolean changeApplicationState(String applicationUuid, String targetState) throws CloudFoundryException {
        log.debug("changeApplicationState to {}", targetState);
        try {
            if (!targetState.equals(getApplicationState(applicationUuid))) {
                cfClient.applicationsV2()
                        .update(
                                UpdateApplicationRequest.builder()
                                        .applicationId(applicationUuid)
                                        .state(targetState)
                                        .build())
                        .get(Config.CF_API_TIMEOUT);
                return true;
            } else {
                log.warn("application {} already in state {}, nothing to do", applicationUuid, targetState);
                return false;
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException {
        log.debug("getApplicationActivity -  {}", appUid);

        //We need to call for appState, lastlogs and lastEvents
        final CountDownLatch latch = new CountDownLatch(3);
        final AtomicReference<Throwable> errorEncountered = new AtomicReference<>(null);
        final AtomicReference<LogMessage> lastLogReference = new AtomicReference<>(null);
        final AtomicReference<ListEventsResponse> lastEventsReference = new AtomicReference<>(null);
        final AtomicReference<GetApplicationResponse> appReference = new AtomicReference<>(null);
        final AtomicReference<Instant> mostRecentLogInstant = new AtomicReference<>(null);

        cfClient.applicationsV2()
                .get(GetApplicationRequest.builder()
                        .applicationId(appUid)
                        .build())
                .subscribe(new BaseSubscriber<>(latch, errorEncountered::set, appReference::set));

        cfClient.events()
                .list(ListEventsRequest.builder()
                        .actee(appUid)
                        .build())
                .subscribe(new BaseSubscriber<>(latch, errorEncountered::set, lastEventsReference::set));

        logClient.recent(RecentLogsRequest.builder()
                .applicationId(appUid)
                .build())
                .subscribe(new BaseSubscriber<>(
                        latch,
                        errorEncountered::set,
                        logMessage -> {
                            //logs are not ordered, must find the most recent
                            Instant msgInstant = logMessage.getTimestamp().toInstant();
                            if (mostRecentLogInstant.get() == null || mostRecentLogInstant.get().isBefore(msgInstant)) {
                                mostRecentLogInstant.set(msgInstant);
                                lastLogReference.set(logMessage);
                            }
                        }
                ));

        return waitForResult(latch, errorEncountered,
                () -> ApplicationActivity.builder()
                        .application(ApplicationIdentity.builder()
                                .guid(appUid)
                                .name(appReference.get().getEntity().getName())
                                .build())
                        .lastEvent(
                                lastEventsReference.get().getResources().isEmpty() ? null
                                        : buildAppEvent(lastEventsReference.get().getResources().get(0)))
                        .lastLog(buildAppLog(lastLogReference.get()))
                        .state(appReference.get().getEntity().getState())
                        .build());
    }

    @Override
    public String getApplicationState(String applicationUuid) throws CloudFoundryException {
        log.debug("getApplicationState");
        try {
            return this.cfClient
                    .applicationsV2()
                    .get(GetApplicationRequest.builder()
                            .applicationId(applicationUuid)
                            .build())
                    .get(Config.CF_API_TIMEOUT)
                    .getEntity().getState();

        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public String getHost(String routeId) throws CloudFoundryException {
        log.debug("getHost");
        GetRouteResponse response = cfClient.routes().get(GetRouteRequest.builder().routeId(routeId).build()).get
                (Config.CF_API_TIMEOUT);
        RouteEntity routeEntity = response.getEntity();
        String route = routeEntity.getHost()+routeEntity.getPath();
        log.debug("route =  {}", route);

        GetDomainResponse domainResponse = cfClient.domains().get(GetDomainRequest.builder().domainId(routeEntity.getDomainId()).build()).get(Config.CF_API_TIMEOUT);
        log.debug("domain = {}",domainResponse.getEntity());
        return route + "." + domainResponse.getEntity().getName();
    }

    @Override
    public List<String> listApplicationRoutes(String applicationUuid) throws CloudFoundryException {
        log.debug("listApplicationRoutes");
        try {
            ListApplicationRoutesResponse response = cfClient.applicationsV2()
                    .listRoutes(
                            ListApplicationRoutesRequest.builder()
                                    .applicationId(applicationUuid)
                                    .build())
                    .get(Config.CF_API_TIMEOUT);
            return response.getResources().stream()
                    .map(routeResource -> routeResource.getMetadata().getId())
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public List<ApplicationIdentity> listApplications(String spaceUuid, Pattern excludeNames) throws
            CloudFoundryException {
        log.debug("listApplications");
        try {
            ListApplicationsResponse response = this.cfClient
                    .applicationsV2()
                    .list(ListApplicationsRequest.builder()
                            .spaceId(spaceUuid)
                            .build())
                    .get(Config.CF_API_TIMEOUT);
            return response.getResources().stream().filter(
                    cloudApplication -> (
                            spaceUuid == null
                                    || spaceUuid.equals(cloudApplication.getEntity().getSpaceId()))
                            && (
                            excludeNames == null
                                    || !excludeNames.matcher(cloudApplication.getEntity().getName()).matches()))
                    .map(
                            cloudApplication ->
                                    ApplicationIdentity.builder()
                                            .guid(cloudApplication.getMetadata().getId())
                                            .name(cloudApplication.getEntity().getName())
                                            .build())
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public List<String> listRouteApplications(String routeUuid) throws CloudFoundryException {
        log.debug("listRouteApplications");
        try {
            ListRouteApplicationsResponse response = cfClient.routes()
                    .listApplications(
                            ListRouteApplicationsRequest.builder()
                                    .routeId(routeUuid)
                                    .build())
                    .get(Config.CF_API_TIMEOUT);
            return response.getResources().stream()
                    .map(appResource -> appResource.getMetadata().getId())
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public boolean startApplication(String applicationUuid) throws CloudFoundryException {
        log.debug("startApplication");
        return changeApplicationState(applicationUuid, CloudFoundryAppState.STARTED);
    }

    @Override
    public boolean stopApplication(String applicationUuid) throws CloudFoundryException {
        log.debug("stopApplication");
        return changeApplicationState(applicationUuid, CloudFoundryAppState.STOPPED);
    }

    @Override
    public void unbind(String bindingId) throws CloudFoundryException {
        cfClient.serviceBindings()
                .delete(DeleteServiceBindingRequest.builder()
                        .serviceBindingId(bindingId)
                        .build())
                .get(Config.CF_API_TIMEOUT);

    }

    private <T> T waitForResult(CountDownLatch latch, AtomicReference<Throwable> errorEncountered,
                                Supplier<T> callback) throws CloudFoundryException {
        try {
            if (!latch.await(Config.CF_API_TIMEOUT.getSeconds(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("subscriber timed out");
            } else if (errorEncountered.get() != null) {
                throw new CloudFoundryException(errorEncountered.get());
            } else {
                if (callback != null) {
                    return callback.get();
                } else {
                    return null;
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            return null;
        }
    }

}
