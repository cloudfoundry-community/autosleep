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
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
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
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.MessageType;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

    static final int CF_INSTANCES_ERROR = 220_001;

    static final int CF_STAGING_NOT_FINISHED = 170_002;

    private static final Envelope FAKE_ENVELOP = Envelope.builder()
            .timestamp(0L)
            .logMessage(LogMessage.builder()
                    .timestamp(0L)
                    .message("dummy")
                    .messageType(MessageType.OUT)
                    .build())
            .eventType(EventType.CONTAINER_METRIC)
            .origin("fake")
            .build();

    @Autowired
    private CloudFoundryClient cloudFoundryClient;

    @Autowired
    private DopplerClient dopplerClient;

    private <T, U> void bind(List<T> objectsToBind, Function<T, Mono<U>> caller)
            throws CloudFoundryException {
        log.debug("bind - {} objects", objectsToBind.size());
        final CountDownLatch latch = new CountDownLatch(objectsToBind.size());
        final AtomicReference<Throwable> errorEncountered = new AtomicReference<>(null);
        Consumer<U> resultConsumer = result -> {
        };
        Consumer<Throwable> errorConsumer = throwable -> {
            errorEncountered.set(throwable);
            latch.countDown();
        };
        objectsToBind.forEach(objectToBind -> caller.apply(objectToBind)
                .subscribe(
                        resultConsumer,
                        errorConsumer,
                        latch::countDown
                ));
        waitForResult(latch, errorEncountered, null);
    }

    @Override
    public void bindApplications(String serviceInstanceId, List<ApplicationIdentity> applications) throws
            CloudFoundryException {
        bind(applications,
                application -> cloudFoundryClient.serviceBindingsV2()
                        .create(
                                CreateServiceBindingRequest
                                        .builder()
                                        .applicationId(application.getGuid())
                                        .serviceInstanceId(serviceInstanceId)
                                        .build()));
    }

    public void bindRoutes(String serviceInstanceId, List<String> routeIds) throws CloudFoundryException {
        bind(routeIds,
                routeId -> cloudFoundryClient.serviceInstances()
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

    private ApplicationInfo.DiagnosticInfo.ApplicationLog buildAppLog(Envelope envelope) {
        return ApplicationInfo.DiagnosticInfo.ApplicationLog.builder()
                .message(envelope.getLogMessage().getMessage())
                .timestamp(Instant.ofEpochSecond(0, getEnvelopeTimestampNanos(envelope)))
                .messageType(envelope.getLogMessage().getMessageType() != null ?
                        envelope.getLogMessage().getMessageType().name() : null)
                .sourceType(envelope.getLogMessage().getSourceType())
                .sourceInstance(envelope.getLogMessage().getSourceInstance())
                .build();
    }

    private boolean changeApplicationState(String applicationUuid, String targetState) throws CloudFoundryException {
        log.debug("changeApplicationState to {}", targetState);
        try {
            if (!targetState.equals(getApplicationState(applicationUuid))) {
                cloudFoundryClient.applicationsV2()
                        .update(
                                UpdateApplicationRequest.builder()
                                        .applicationId(applicationUuid)
                                        .state(targetState)
                                        .build())
                        .block(Config.CF_API_TIMEOUT);
                return true;
            } else {
                log.warn("application {} already in state {}, nothing to do", applicationUuid, targetState);
                return false;
            }
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    private boolean envelopeContainsRequiredFields(Envelope envelope) {
        return envelope.getLogMessage() != null &&
                (envelope.getTimestamp() != null || envelope.getLogMessage().getTimestamp() != null);
    }

    @Override
    public ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException {
        log.debug("getApplicationActivity -  {}", appUid);

        //We need to call for appState, lastlogs and lastEvents
        final CountDownLatch latch = new CountDownLatch(3);
        final AtomicReference<Throwable> errorEncountered = new AtomicReference<>(null);
        final AtomicReference<Envelope> lastLogReference = new AtomicReference<>(FAKE_ENVELOP);
        final AtomicReference<ListEventsResponse> lastEventsReference = new AtomicReference<>(null);
        final AtomicReference<GetApplicationResponse> appReference = new AtomicReference<>(null);

        Consumer<Throwable> errorConsumer = throwable -> {
            errorEncountered.set(throwable);
            latch.countDown();
        };

        cloudFoundryClient.applicationsV2()
                .get(GetApplicationRequest.builder()
                        .applicationId(appUid)
                        .build())
                .subscribe(appReference::set,
                        errorConsumer,
                        latch::countDown);

        cloudFoundryClient.events()
                .list(ListEventsRequest.builder()
                        .actee(appUid)
                        .build())
                .subscribe(lastEventsReference::set,
                        errorConsumer,
                        latch::countDown);

        dopplerClient.recentLogs(RecentLogsRequest.builder()
                .applicationId(appUid)
                .build())
                .filter(this::envelopeContainsRequiredFields)
                .reduce(lastLogReference.get(),
                        this::getNewestEnvelope)
                .subscribe(lastLogReference::set,
                        errorConsumer,
                        latch::countDown);
        return waitForResult(latch,
                errorEncountered,
                () -> ApplicationActivity.builder()
                        .application(ApplicationIdentity.builder()
                                .guid(appUid)
                                .name(appReference.get().getEntity().getName())
                                .build())
                        .lastEvent(
                                lastEventsReference.get().getResources().isEmpty() ? null
                                        : buildAppEvent(lastEventsReference.get().getResources().get(0)))
                        .lastLog(lastLogReference.get() != FAKE_ENVELOP ? buildAppLog(lastLogReference.get()) : null)
                        .state(appReference.get().getEntity().getState())
                        .build());
    }

    private Mono<ApplicationInstancesResponse> getApplicationInstances(String applicationUuid) {
        log.debug("listApplicationRoutes");
        Predicate<Throwable> noInstanceError = throwable ->
                throwable instanceof org.cloudfoundry.client.v2.CloudFoundryException
                        && ((org.cloudfoundry.client.v2.CloudFoundryException) throwable).getCode()
                        == CF_INSTANCES_ERROR;

        Predicate<Throwable> noStagingError = throwable ->
                throwable instanceof org.cloudfoundry.client.v2.CloudFoundryException
                        && ((org.cloudfoundry.client.v2.CloudFoundryException) throwable).getCode()
                        == CF_STAGING_NOT_FINISHED;

        return cloudFoundryClient.applicationsV2()
                .instances(
                        ApplicationInstancesRequest.builder()
                                .applicationId(applicationUuid)
                                .build())
                .otherwise(noInstanceError, throwable -> Mono.just(ApplicationInstancesResponse.builder().build()))
                .otherwise(noStagingError, throwable -> Mono.just(ApplicationInstancesResponse.builder().build()));
    }

    @Override
    public String getApplicationState(String applicationUuid) throws CloudFoundryException {
        log.debug("getApplicationState");
        try {
            return this.cloudFoundryClient
                    .applicationsV2()
                    .get(GetApplicationRequest.builder()
                            .applicationId(applicationUuid)
                            .build())
                    .block(Config.CF_API_TIMEOUT)
                    .getEntity().getState();

        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    private Long getEnvelopeTimestampNanos(Envelope envelope) {
        return (envelope.getLogMessage().getTimestamp() != null ? envelope.getLogMessage().getTimestamp()
                : envelope.getTimestamp()) ;
    }

    @Override
    public String getHost(String routeId) throws CloudFoundryException {
        try {
            log.debug("getHost");
            GetRouteResponse response = cloudFoundryClient.routes()
                    .get(GetRouteRequest.builder()
                            .routeId(routeId)
                            .build())
                    .block(Config.CF_API_TIMEOUT);
            RouteEntity routeEntity = response.getEntity();
            String route = routeEntity.getHost() + routeEntity.getPath();
            log.debug("route =  {}", route);

            GetDomainResponse domainResponse = cloudFoundryClient.domains()
                    .get(GetDomainRequest.builder()
                            .domainId(routeEntity.getDomainId())
                            .build())
                    .block(Config.CF_API_TIMEOUT);
            log.debug("domain = {}", domainResponse.getEntity());
            return route + "." + domainResponse.getEntity().getName();
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    private Envelope getNewestEnvelope(Envelope previous, Envelope current) {
        //for first iteration
        if (getEnvelopeTimestampNanos(previous) < getEnvelopeTimestampNanos(current)) {
            return current;
        } else {
            return previous;
        }

    }

    @Override
    public boolean isAppRunning(String appUid) throws CloudFoundryException {
        log.debug("isAppRunning");
        try {
            return !getApplicationInstances(appUid)
                    .flatMap(response -> Flux.fromIterable(response.getInstances().values()))
                    .filter(instanceInfo -> "RUNNING".equals(instanceInfo.getState()))
                    .collect(ArrayList<ApplicationInstanceInfo>::new, ArrayList::add)
                    .block(Config.CF_API_TIMEOUT)
                    .isEmpty();

        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public List<ApplicationIdentity> listAliveApplications(String spaceUuid, Pattern excludeNames) throws
            CloudFoundryException {
        log.debug("listAliveApplications from space_guid:" + spaceUuid);
        try {
            return Mono.just(spaceUuid)
                    .then(spaceId -> this.cloudFoundryClient
                            .applicationsV2()
                            .list(ListApplicationsRequest.builder()
                                    .spaceId(spaceUuid)
                                    .build()))
                    .flatMap(listApplicationsResponse -> Flux.fromIterable(listApplicationsResponse.getResources()))
                    //remove all filtered applications
                    .filter(applicationResource -> excludeNames == null
                            || !excludeNames.matcher(applicationResource.getEntity().getName()).matches())
                    //get instances
                    .flatMap(applicationResource -> Mono.when(Mono.just(applicationResource),
                            getApplicationInstances(applicationResource.getMetadata().getId())))
                    //filter the one that has no instances (ie. STOPPED)
                    .filter(tuple -> !tuple.getT2().getInstances().isEmpty())
                    .map(tuple -> ApplicationIdentity.builder()
                            .guid(tuple.getT1().getMetadata().getId())
                            .name(tuple.getT1().getEntity().getName())
                            .build())
                    .collect(ArrayList<ApplicationIdentity>::new, ArrayList::add)
                    .block(Config.CF_API_TIMEOUT);
        } catch (RuntimeException r) {
            throw new CloudFoundryException("failed listing applications from space_id: " + spaceUuid, r);
        }
    }

    @Override
    public List<String> listApplicationRoutes(String applicationUuid) throws CloudFoundryException {
        log.debug("listApplicationRoutes");
        try {
            ListApplicationRoutesResponse response = cloudFoundryClient.applicationsV2()
                    .listRoutes(
                            ListApplicationRoutesRequest.builder()
                                    .applicationId(applicationUuid)
                                    .build())
                    .block(Config.CF_API_TIMEOUT);
            return response.getResources().stream()
                    .map(routeResource -> routeResource.getMetadata().getId())
                    .collect(Collectors.toList());
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
    }

    @Override
    public List<String> listRouteApplications(String routeUuid) throws CloudFoundryException {
        log.debug("listRouteApplications");
        try {
            ListRouteApplicationsResponse response = cloudFoundryClient.routes()
                    .listApplications(
                            ListRouteApplicationsRequest.builder()
                                    .routeId(routeUuid)
                                    .build())
                    .block(Config.CF_API_TIMEOUT);
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
        try {
            cloudFoundryClient.serviceBindingsV2()
                    .delete(DeleteServiceBindingRequest.builder()
                            .serviceBindingId(bindingId)
                            .build())
                    .block(Config.CF_API_TIMEOUT);
        } catch (RuntimeException r) {
            throw new CloudFoundryException(r);
        }
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
