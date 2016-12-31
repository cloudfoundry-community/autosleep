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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationActivity;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationIdentity;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Resource.Metadata;
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
import org.cloudfoundry.client.v2.organizations.OrganizationEntity.OrganizationEntityBuilder;
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
import org.cloudfoundry.client.v2.servicebindings.ServiceBindings;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceToRouteRequest;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceToRouteResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstances;
import org.cloudfoundry.logging.LogMessage;
import org.cloudfoundry.logging.LogMessage.MessageType;
import org.cloudfoundry.logging.LoggingClient;
import org.cloudfoundry.logging.RecentLogsRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryApiTest {

    @Mock
    private CloudFoundryClient cfClient;

    @InjectMocks
    private CloudFoundryApi cloudFoundryApi;

    @Mock
    private LoggingClient logClient;

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
        ServiceBindings serviceBindings = mock(ServiceBindings.class);
        when(cfClient.serviceBindings()).thenReturn(serviceBindings);
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
        ServiceBindings serviceBindings = mock(ServiceBindings.class);
        when(cfClient.serviceBindings()).thenReturn(serviceBindings);
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
        when(serviceInstances.bindToRoute(any(BindServiceInstanceToRouteRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));

        verifyThrown(() -> cloudFoundryApi.bindRoutes("serviceInstanceId",
                Collections.singletonList("route-id")),
                CloudFoundryException.class);
    }

    @Test
    public void test_bind_routes_should_succeed() throws CloudFoundryException {
        ServiceInstances serviceInstances = mock(ServiceInstances.class);
        when(cfClient.serviceInstances()).thenReturn(serviceInstances);
        when(serviceInstances.bindToRoute(any(BindServiceInstanceToRouteRequest.class)))
                .thenReturn(Mono.just(BindServiceInstanceToRouteResponse.builder()
                        .metadata(Metadata.builder()
                                .build())
                        .entity(ServiceInstanceEntity.builder()
                                .build())
                        .build()));
        cloudFoundryApi.bindRoutes("serviceInstanceId",
                Collections.singletonList("route-id"));
        verify(serviceInstances, times(1)).bindToRoute(any(BindServiceInstanceToRouteRequest.class));
    }

    @Test
    public void test_get_application_activity() throws CloudFoundryException {
        final String applicationId = "application-id";
        final String applicationState = "RUNNING";
        final String applicationName = "application-name";
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        Events events = mock(Events.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(cfClient.events()).thenReturn(events);

        mockGetApplication(applications, applicationName, applicationState);


        Instant eventTimestamp = Instant.now().minus(Duration.ofDays(3));

        when(events.list(any(ListEventsRequest.class)))
                .thenReturn(Mono.just(ListEventsResponse.builder()
                        .resource(EventResource.builder()
                                .metadata(Metadata.builder().build())
                                .entity(EventEntity.builder()
                                        .actee("event-actee-test")
                                        .actor("event-actor-test")
                                        .type("event-type-test")
                                        .timestamp(eventTimestamp.toString())
                                        .build())
                                .build())
                        .build()));

        long lastTimestamp = System.currentTimeMillis();
        Function<Integer, LogMessage> logMessageBuilder = diff -> LogMessage.builder()
                .timestamp(new Date(lastTimestamp - diff))
                .message("message-" + diff)
                .applicationId("application-id")
                .messageType(MessageType.ERR)
                .sourceId("source-id")
                .sourceName("source-name")
                .build();

        when(logClient.recent(any(RecentLogsRequest.class)))
                .thenReturn(Flux.fromIterable(
                        Arrays.asList(logMessageBuilder.apply(1),
                                logMessageBuilder.apply(2),
                                logMessageBuilder.apply(3),
                                logMessageBuilder.apply(0),
                                logMessageBuilder.apply(4))
                ));

        ApplicationActivity activity = cloudFoundryApi.getApplicationActivity(applicationId);
        assertNotNull(activity);
        assertNotNull(activity.getApplication());
        assertEquals(applicationId, activity.getApplication().getGuid());
        assertEquals(applicationName, activity.getApplication().getName());
        assertNotNull(activity.getLastEvent());
        assertEquals(eventTimestamp, activity.getLastEvent().getTimestamp());
        assertNotNull(activity.getLastLog());
        assertEquals(Instant.ofEpochMilli(lastTimestamp), activity.getLastLog().getTimestamp());
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
                .thenReturn(Mono.error(new org.cloudfoundry.client.v2.CloudFoundryException(666, "", "")));
    }

    private void test_is_app_running_should_return_false(int code) throws CloudFoundryException {
        ApplicationsV2 applications = mock(ApplicationsV2.class);
        when(cfClient.applicationsV2()).thenReturn(applications);
        when(applications.instances(any(ApplicationInstancesRequest.class)))
                .thenReturn(Mono.error(new org.cloudfoundry.client.v2.CloudFoundryException(code, "", "")));
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
                        return Mono.error(new org.cloudfoundry.client.v2.CloudFoundryException(
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
        ServiceBindings serviceBindings = mock(ServiceBindings.class);
        when(cfClient.serviceBindings()).thenReturn(serviceBindings);
        when(serviceBindings.delete(any(DeleteServiceBindingRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));
        verifyThrown(() -> cloudFoundryApi.unbind("service-binding-id"),
                CloudFoundryException.class);
    }

    @Test
    public void test_unbind_application_should_succeed() throws CloudFoundryException {
        ServiceBindings serviceBindings = mock(ServiceBindings.class);
        when(cfClient.serviceBindings()).thenReturn(serviceBindings);
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
                Mono.error(new org.cloudfoundry.client.v2.CloudFoundryException(anyOtherErrorCode,
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
                .thenReturn(Mono.error(new org.cloudfoundry.client.v2.CloudFoundryException(
                        cloudFoundryApi.CF_ORGANIZATION_NOT_FOUND, fakeOrgId, fakeOrgId)));

        assertFalse(cloudFoundryApi.isValidOrganization(fakeOrgId));
    }

}