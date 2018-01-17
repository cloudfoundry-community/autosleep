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

import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationActivity;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationIdentity;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationResponse;
import org.cloudfoundry.client.v2.domains.DomainEntity;
import org.cloudfoundry.client.v2.domains.Domains;
import org.cloudfoundry.client.v2.domains.GetDomainRequest;
import org.cloudfoundry.client.v2.domains.GetDomainResponse;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.EventResource;
import org.cloudfoundry.client.v2.events.Events;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.organizations.Organizations;
import org.cloudfoundry.client.v2.routes.GetRouteRequest;
import org.cloudfoundry.client.v2.routes.GetRouteResponse;
import org.cloudfoundry.client.v2.routes.ListRouteApplicationsRequest;
import org.cloudfoundry.client.v2.routes.ListRouteApplicationsResponse;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.routes.RouteResource;
import org.cloudfoundry.client.v2.routes.Routes;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingResponse;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingsV2;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceRouteRequest;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceRouteResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstances;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.MessageType;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryApiTest {

    @Mock
    private CloudFoundryClient cfClient;

    @InjectMocks
    private CloudFoundryApi cloudFoundryApi;

    @Mock
    private DopplerClient dopplerClient;

    private void mockGetApplication(ApplicationsV2 mockApplications, String name, String applicationState) {
        when(mockApplications.get(any(GetApplicationRequest.class)))
                .thenReturn(Mono.just(GetApplicationResponse.builder()
                        .metadata(Metadata.builder().build())
                        .entity(ApplicationEntity.builder()
                                .name(name)
                                .state(applicationState)
                                .build())
                        .build()));
    }

    @Test
    public void test_bind_applications_should_fail() throws CloudFoundryException {
        ServiceBindingsV2 serviceBindings = mock(ServiceBindingsV2.class);
        when(cfClient.serviceBindingsV2()).thenReturn(serviceBindings);
        when(serviceBindings.create(any(CreateServiceBindingRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));
        verifyThrown(() -> cloudFoundryApi.bindApplications("service-instance-id",
                Collections.singletonList(ApplicationIdentity.builder()
                        .guid("application-id")
                        .name("application-name")
                        .build())),
                CloudFoundryException.class);
    }

    @Test
    public void test_bind_applications_should_succeed() throws CloudFoundryException {
        ServiceBindingsV2 serviceBindings = mock(ServiceBindingsV2.class);
        when(cfClient.serviceBindingsV2()).thenReturn(serviceBindings);
        when(serviceBindings.create(any(CreateServiceBindingRequest.class)))
                .thenReturn(Mono.just(CreateServiceBindingResponse.builder()
                        .metadata(Metadata.builder()
                                .build())
                        .entity(ServiceBindingEntity.builder()
                                .build())
                        .build()));
        cloudFoundryApi.bindApplications("service-instance-id",
                Collections.singletonList(ApplicationIdentity.builder()
                        .guid("application-id")
                        .name("application-name")
                        .build()));
        verify(serviceBindings, times(1)).create(any(CreateServiceBindingRequest.class));
    }

    @Test
    public void test_bind_routes_should_fail() throws CloudFoundryException {
        ServiceInstances serviceInstances = mock(ServiceInstances.class);
        when(cfClient.serviceInstances()).thenReturn(serviceInstances);
        when(serviceInstances.bindRoute(any(BindServiceInstanceRouteRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));

        verifyThrown(() -> cloudFoundryApi.bindRoutes("serviceInstanceId",
                Collections.singletonList("route-id")),
                CloudFoundryException.class);
    }

    @Test
    public void test_bind_routes_should_succeed() throws CloudFoundryException {
        ServiceInstances serviceInstances = mock(ServiceInstances.class);
        when(cfClient.serviceInstances()).thenReturn(serviceInstances);
        when(serviceInstances.bindRoute(any(BindServiceInstanceRouteRequest.class)))
                .thenReturn(Mono.just(BindServiceInstanceRouteResponse.builder()
                        .metadata(Metadata.builder()
                                .build())
                        .entity(ServiceInstanceEntity.builder()
                                .build())
                        .build()));
        cloudFoundryApi.bindRoutes("serviceInstanceId",
                Collections.singletonList("route-id"));
        verify(serviceInstances, times(1)).bindRoute(any(BindServiceInstanceRouteRequest.class));
    }

    @Test
    public void test_it_extracts_timestamps_from_log_messages() {
        //Make sure we properly understand Java Instant API
        Instant now = Instant.now();
        long timestampNanos = now.getEpochSecond() * 1000000000 + now.getNano();
        Instant instantFromNanos = Instant.ofEpochSecond(0, timestampNanos);
        assertEquals(now, instantFromNanos);

        //Then make sure the timestamp returned by CF is of the same format
        //given a log message
        //$ cf logs --recent hello-cf-java-client
        // Connected, dumping recent logs for app hello-cf-java-client in org bercheg-org / space gberche-dev-box as
        //  2017-05-19T15:32:52.63+0200 [APP/PROC/WEB/0]OUT 82.122.232.9, 10.10.66.216 - - - [19/May/2017:13:32:52 +0000] "GET / HTTP/1.1" 200 6
        //   envelope timestamp                                                                log message timestamp
        //   local time formatting                                                             UTC formatting
        // for which the API returned the following timestamp long
        long logMessageTimestamp = 1495200772630779308L;
        //then extracted instant should match including timezone conversion
        Instant instantFromLogMessage = cloudFoundryApi.getInstantFromLogMessageTimestamp(logMessageTimestamp);
        assertEquals("2017-05-19T13:32:52.630779308Z", instantFromLogMessage.toString());
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // we document real-life IPs to illustrate returned CF API values
    public void test_get_application_activity() throws CloudFoundryException {
        final String applicationId = "application-id";
        final String applicationState = "RUNNING";
        final String applicationName = "application-name";
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        Events events = mock(Events.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(cfClient.events()).thenReturn(events);

        mockGetApplication(applications, applicationName, applicationState);


        Instant now = Instant.now();
        Instant eventTimestamp = now.minus(Duration.ofDays(3));

        //given typical event request we make
        //        ListEventsRequest eventsRequest = ListEventsRequest.builder()
        //                .actee("43dc8103-6d62-46c7-8456-6dcebfa2f2d1") //app Guid
        //                .build();
        //CF is returning the following real-life recorded values
        when(events.list(any(ListEventsRequest.class)))
                .thenReturn(Mono.just(ListEventsResponse.builder()
                        .resource(EventResource.builder()
                                .metadata(Metadata.builder().build())
                                .entity(EventEntity.builder()
                                        .actee("43dc8103-6d62-46c7-8456-6dcebfa2f2d1") //app Guid
                                        .acteeName("hello-cf-java-client") //app name
                                        .acteeType("app")
                                        .actor("526cc2f7-3d07-4e04-85b2-e3c96282541c") //user Guid
                                        .actorName("user@email.com")
                                        .acteeType("user")
                                        .organizationId("37816bdf-a476-473d-9ae4-75d7cfefabca")
                                        .spaceId("3518c030-0b99-4075-834e-1b47e6ec7909")
                                        .timestamp(eventTimestamp.toString()) //String "2017-03-14T08:21:22Z" returned by PWS
                                        .type("audit.app.create")
                                        .build())
                                .build())
                        .build()));

        long lastTimestampNanos = now.getEpochSecond() * 1000000000 + now.getNano();
        Function<Integer, Envelope> envelopeBuilder = diff -> Envelope.builder()
                .deployment("cf-cfapps-io2-diego") //bosh deployment name
                .eventType(EventType.LOG_MESSAGE)
                .index("3e49c9d7-8e67-4dc1-8672-ebc2299e261f") //log index
                .ip("10.10.148.105") //diego cell IP
                .job("cell_2xl_z2")  //diego cell job name
                .logMessage(LogMessage.builder()
                        .applicationId("application-id")
                        .message("message-" + diff)
                        .messageType(MessageType.OUT)
                        .sourceInstance("0")
                        .sourceType("CELL")
                        .timestamp(lastTimestampNanos + diff) //timestamp at which the log event was written by the app
                        //eg 1495195840768215528 for 2017-05-19T12:10:40.768215528Z
                        .build())
                .origin("rep") //diego rep component
                .tags(new HashMap<>())
                .timestamp(lastTimestampNanos + diff) //envelope timestamp
                .valueMetric(null)
                .build();

        //Given a recent log response with 5 logs, arriving out of order through the log pipeline
        // w.r.t. their original enveloppe + log message timestamp
        //and the most recent event by 4 ns
                when(dopplerClient.recentLogs(any(RecentLogsRequest.class)))
                .thenReturn(Flux.fromIterable(
                        Arrays.asList(envelopeBuilder.apply(1),
                                envelopeBuilder.apply(2),
                                envelopeBuilder.apply(3),
                                envelopeBuilder.apply(0),
                                envelopeBuilder.apply(4))
                ));

        ApplicationActivity activity = cloudFoundryApi.getApplicationActivity(applicationId);
        assertNotNull(activity);
        assertNotNull(activity.getApplication());
        assertEquals(applicationId, activity.getApplication().getGuid());
        assertEquals(applicationName, activity.getApplication().getName());
        assertNotNull(activity.getLastEvent());
        assertEquals(eventTimestamp, activity.getLastEvent().getTimestamp());
        assertEquals("last event ordered by timestamp be the most recent by 4 nanosecs", Instant.ofEpochSecond(0, lastTimestampNanos +4), activity.getLastLog().getTimestamp());
        assertEquals("message-" + 4, activity.getLastLog().getMessage());
    }

    @Test
    public void test_get_application_state_should_fail() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);

        when(applications.get(any(GetApplicationRequest.class)))
                .thenReturn(Mono.error(new RuntimeException()));
        verifyThrown(() -> cloudFoundryApi.getApplicationState("application-id"),
                CloudFoundryException.class);
    }

    @Test
    public void test_get_application_state_should_succeed() throws CloudFoundryException {
        final String applicationState = "RUNNING";
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", applicationState);
        String result = cloudFoundryApi.getApplicationState("application-id");
        verify(applications, times(1)).get(any(GetApplicationRequest.class));
        assertEquals(applicationState, result);
    }

    @Test
    public void test_get_host_by_route_id_should_fail() throws CloudFoundryException {
        Routes routes = mock(Routes.class);
        when(cfClient.routes()).thenReturn(routes);
        Domains domains = mock(Domains.class);
        when(cfClient.domains()).thenReturn(domains);
        when(routes.get(any(GetRouteRequest.class)))
                .thenThrow(new RuntimeException());
        verifyThrown(() -> cloudFoundryApi.getHost("route-id"),
                CloudFoundryException.class);
    }

    @Test
    public void test_get_host_by_route_id_should_succeed() throws CloudFoundryException {
        final String host = "somewhere";
        final String domain = "the.rainbow";
        final String path = ".over";
        final String domainId = "domain-id";
        Routes routes = mock(Routes.class);
        when(cfClient.routes()).thenReturn(routes);
        Domains domains = mock(Domains.class);
        when(cfClient.domains()).thenReturn(domains);
        when(routes.get(any(GetRouteRequest.class)))
                .thenReturn(Mono.just(GetRouteResponse.builder()
                        .metadata(Metadata.builder().build())
                        .entity(RouteEntity.builder()
                                .host(host)
                                .path(path)
                                .domainId(domainId)
                                .build())
                        .build()));
        when(domains.get(any(GetDomainRequest.class)))
                .then(invocation -> {
                    GetDomainRequest request = (GetDomainRequest) invocation.getArguments()[0];
                    assertEquals(domainId, request.getDomainId());
                    return Mono.just(GetDomainResponse.builder()
                            .metadata(Metadata.builder().build())
                            .entity(DomainEntity.builder()
                                    .name(domain)
                                    .build())
                            .build());
                });
        String result = cloudFoundryApi.getHost("route-id");

        assertEquals(host + path + "." + domain, result);
    }

    @Test
    public void test_is_app_running_should_fail() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.instances(any(ApplicationInstancesRequest.class)))
                .thenReturn(Mono.error(new org.cloudfoundry.client.v2.ClientV2Exception(null, 666, "", "")));
    }

    private void test_is_app_running_should_return_false(int code) throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.instances(any(ApplicationInstancesRequest.class)))
                .thenReturn(Mono.error(new org.cloudfoundry.client.v2.ClientV2Exception(null, code, "", "")));
        assertFalse(cloudFoundryApi.isAppRunning("application-id"));
        verify(applications, times(1)).instances(any(ApplicationInstancesRequest.class));
    }

    @Test
    public void test_is_app_running_should_return_false_due_to_instance_error() throws CloudFoundryException {
        test_is_app_running_should_return_false(CloudFoundryApi.CF_INSTANCES_ERROR);
    }

    @Test
    public void test_is_app_running_should_return_false_due_to_staging_not_finished() throws CloudFoundryException {
        test_is_app_running_should_return_false(CloudFoundryApi.CF_STAGING_NOT_FINISHED);
    }

    @Test
    public void test_is_app_running_should_return_true() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.instances(any(ApplicationInstancesRequest.class)))
                .thenReturn(Mono.just(ApplicationInstancesResponse.builder()
                        .instance("1", ApplicationInstanceInfo.builder()
                                .state("STAGING")
                                .build())
                        .instance("2", ApplicationInstanceInfo.builder()
                                .state("STAGING")
                                .build())
                        .instance("3", ApplicationInstanceInfo.builder()
                                .state("CRASHED")
                                .build())
                        .instance("4", ApplicationInstanceInfo.builder()
                                .state("RUNNING")
                                .build())
                        .build()));
        assertTrue(cloudFoundryApi.isAppRunning("application-id"));
        verify(applications, times(1)).instances(any(ApplicationInstancesRequest.class));
    }

    @Test
    public void test_list_alive_applications() throws CloudFoundryException {
        final String returnedApplication = "application-returned";
        final String ignoredApplication = "application-ignored";
        final String stoppedApplicationId = "application-stopped";

        Pattern excludePattern = Pattern.compile("^(?:(?!" + returnedApplication + ").)*$");

        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.list(any(ListApplicationsRequest.class)))
                .thenReturn(Mono.just(ListApplicationsResponse.builder()
                        .resource(ApplicationResource.builder()
                                .metadata(Metadata.builder()
                                        .id(returnedApplication)
                                        .build())
                                .entity(ApplicationEntity.builder()
                                        .name(returnedApplication)
                                        .build())
                                .build())
                        .resource(ApplicationResource.builder()
                                .metadata(Metadata.builder()
                                        .id(ignoredApplication)
                                        .build())
                                .entity(ApplicationEntity.builder()
                                        .name(ignoredApplication)
                                        .build())
                                .build())
                        .resource(ApplicationResource.builder()
                                .metadata(Metadata.builder()
                                        .id(stoppedApplicationId)
                                        .build())
                                .entity(ApplicationEntity.builder()
                                        .name(stoppedApplicationId)
                                        .build())
                                .build())
                        .build()));
        when(applications.instances(any(ApplicationInstancesRequest.class)))
                .then(invocation -> {
                    ApplicationInstancesRequest request = (ApplicationInstancesRequest) invocation.getArguments()[0];
                    if (request.getApplicationId().equals(stoppedApplicationId)) {
                        return Mono.error(new org.cloudfoundry.client.v2.ClientV2Exception(
                                null,
                                CloudFoundryApi.CF_INSTANCES_ERROR,
                                "",
                                ""));
                    } else {
                        return Mono.just(ApplicationInstancesResponse.builder()
                                .instance("1", ApplicationInstanceInfo.builder()
                                        .state("RUNNING")
                                        .build())
                                .build());
                    }
                });
        List<ApplicationIdentity> result = cloudFoundryApi
                .listAliveApplications("space-id", excludePattern);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(returnedApplication, result.get(0).getGuid());

    }

    @Test
    public void test_list_applications_of_route_should_fail() throws CloudFoundryException {
        Routes routes = mock(Routes.class);
        when(cfClient.routes()).thenReturn(routes);
        when(routes.listApplications(any(ListRouteApplicationsRequest.class)))
                .thenReturn(Mono.error(new RuntimeException()));
        verifyThrown(() -> cloudFoundryApi.listRouteApplications("route-id"),
                CloudFoundryException.class);
    }

    @Test
    public void test_list_applications_of_route_should_succeed() throws CloudFoundryException {
        Routes routes = mock(Routes.class);
        when(cfClient.routes()).thenReturn(routes);
        when(routes.listApplications(any(ListRouteApplicationsRequest.class)))
                .thenReturn(Mono.just(ListRouteApplicationsResponse.builder()
                        .resource(ApplicationResource.builder()
                                .metadata(Metadata.builder()
                                        .id("application-id-1")
                                        .build())
                                .build())
                        .resource(ApplicationResource.builder()
                                .metadata(Metadata.builder()
                                        .id("application-id-2")
                                        .build())
                                .build())
                        .build()
                ));
        List<String> routeIds = cloudFoundryApi.listRouteApplications("route-id");
        verify(routes, times(1)).listApplications(any(ListRouteApplicationsRequest.class));
        assertNotNull(routeIds);
        assertEquals(2, routeIds.size());
        assertEquals("application-id-1", routeIds.get(0));
        assertEquals("application-id-2", routeIds.get(1));
    }

    @Test
    public void test_list_route_of_application_should_fail() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.listRoutes(any(ListApplicationRoutesRequest.class)))
                .thenReturn(Mono.error(new RuntimeException()));
        verifyThrown(() -> cloudFoundryApi.listApplicationRoutes("application-id"),
                CloudFoundryException.class);
    }

    @Test
    public void test_list_routes_of_application_should_succeed() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.listRoutes(any(ListApplicationRoutesRequest.class)))
                .thenReturn(Mono.just(ListApplicationRoutesResponse.builder()
                        .resource(RouteResource.builder()
                                .metadata(Metadata.builder()
                                        .id("route-id-1")
                                        .build())
                                .entity(RouteEntity.builder().build())
                                .build())
                        .resource(RouteResource.builder()
                                .metadata(Metadata.builder()
                                        .id("route-id-2")
                                        .build())
                                .entity(RouteEntity.builder().build())
                                .build())
                        .build()));
        List<String> routeIds = cloudFoundryApi.listApplicationRoutes("application-id");
        verify(applications, times(1)).listRoutes(any(ListApplicationRoutesRequest.class));
        assertNotNull(routeIds);
        assertEquals(2, routeIds.size());
        assertEquals("route-id-1", routeIds.get(0));
        assertEquals("route-id-2", routeIds.get(1));
    }

    @Test
    public void test_start_application_should_fail() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", "STOPPED");
        when(cfClient.applicationsV2()
                .update(
                        any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));
        verifyThrown(() -> cloudFoundryApi.startApplication("application-id"),
                CloudFoundryException.class);

    }

    @Test
    public void test_start_application_should_not_start_a_started_application() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", "STARTED");
        when(cfClient.applicationsV2()
                .update(
                        any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.just(UpdateApplicationResponse.builder().build()));
        assertFalse(cloudFoundryApi.startApplication("application-id"));
        verify(applications, times(1)).get(any(GetApplicationRequest.class));
        verify(applications, never()).update(any(UpdateApplicationRequest.class));

    }

    @Test
    public void test_start_application_should_start_a_stopped_application() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", "STOPPED");
        when(cfClient.applicationsV2()
                .update(
                        any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.just(UpdateApplicationResponse.builder().build()));
        assertTrue(cloudFoundryApi.startApplication("application-id"));
        verify(applications, times(1)).get(any(GetApplicationRequest.class));
        verify(applications, times(1)).update(any(UpdateApplicationRequest.class));

    }

    @Test
    public void test_stop_application_should_fail() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", "STARTED");
        when(cfClient.applicationsV2()
                .update(
                        any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));
        verifyThrown(() -> cloudFoundryApi.stopApplication("application-id"),
                CloudFoundryException.class);

    }

    @Test
    public void test_stop_application_should_not_stop_a_stopped_application() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", "STOPPED");
        when(cfClient.applicationsV2()
                .update(
                        any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.just(UpdateApplicationResponse.builder().build()));
        assertFalse(cloudFoundryApi.stopApplication("application-id"));
        verify(applications, times(1)).get(any(GetApplicationRequest.class));
        verify(applications, never()).update(any(UpdateApplicationRequest.class));

    }

    @Test
    public void test_stop_application_should_stop_a_started_application() throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        mockGetApplication(applications, "name", "STARTED");
        when(cfClient.applicationsV2()
                .update(
                        any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.just(UpdateApplicationResponse.builder().build()));
        assertTrue(cloudFoundryApi.stopApplication("application-id"));
        verify(applications, times(1)).get(any(GetApplicationRequest.class));
        verify(applications, times(1)).update(any(UpdateApplicationRequest.class));

    }

    @Test
    public void test_unbind_application_should_fail() throws CloudFoundryException {
        ServiceBindingsV2 serviceBindings = mock(ServiceBindingsV2.class);
        when(cfClient.serviceBindingsV2()).thenReturn(serviceBindings);
        when(serviceBindings.delete(any(DeleteServiceBindingRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));
        verifyThrown(() -> cloudFoundryApi.unbind("service-binding-id"),
                CloudFoundryException.class);
    }

    @Test
    public void test_unbind_application_should_succeed() throws CloudFoundryException {
        ServiceBindingsV2 serviceBindings = mock(ServiceBindingsV2.class);
        when(cfClient.serviceBindingsV2()).thenReturn(serviceBindings);
        when(serviceBindings.delete(any(DeleteServiceBindingRequest.class)))
                .thenReturn(Mono.empty());
        cloudFoundryApi.unbind("service-binding-id");
        verify(serviceBindings, times(1)).delete(any(DeleteServiceBindingRequest.class));
    }

    @Test
    public void test_isValidOrganization_should_return_true_for_valid_organization()
            throws CloudFoundryException {
        String fakeOrgId = "organization-guid";
        Organizations organizations = mock(Organizations.class);
        when(cfClient.organizations()).thenReturn(organizations);
        GetOrganizationRequest request = GetOrganizationRequest.builder().organizationId(fakeOrgId)
                .build();
        GetOrganizationResponse response = GetOrganizationResponse.builder()
                .metadata(Metadata.builder().id(fakeOrgId).build())
                .entity(OrganizationEntity.builder().name("organization-name").build()).build();
        when(organizations.get(request)).thenReturn(Mono.just(response));
        assertTrue(cloudFoundryApi.isValidOrganization(fakeOrgId));
    }

    @Test
    public void test_isValidOrganization_should_throw_exception_for_any_other_error()
            throws CloudFoundryException {
        String fakeOrgId = "incorrect-organization-guid";
        int anyOtherErrorCode = 30004;
        Organizations organizations = mock(Organizations.class);
        when(cfClient.organizations()).thenReturn(organizations);
        GetOrganizationRequest request = GetOrganizationRequest.builder().organizationId(fakeOrgId)
                .build();
        when(organizations.get(request)).thenReturn(
                Mono.error(new org.cloudfoundry.client.v2.ClientV2Exception(null, anyOtherErrorCode,
                        fakeOrgId, fakeOrgId)));

        verifyThrown(() -> cloudFoundryApi.isValidOrganization(fakeOrgId),
                Throwable.class);
    }

    @Test
    public void test_isValidOrganization_should_return_false_for_invalid_organization()
            throws CloudFoundryException {
        String fakeOrgId = "incorrect-organization-guid";
        Organizations organizations = mock(Organizations.class);
        when(cfClient.organizations()).thenReturn(organizations);
        GetOrganizationRequest request = GetOrganizationRequest.builder().organizationId(fakeOrgId)
                .build();
        when(organizations.get(request))
                .thenReturn(Mono.error(new org.cloudfoundry.client.v2.ClientV2Exception(null,
                        cloudFoundryApi.CF_ORGANIZATION_NOT_FOUND, fakeOrgId, fakeOrgId)));

        assertFalse(cloudFoundryApi.isValidOrganization(fakeOrgId));
    }

}