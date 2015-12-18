package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException.EntityType;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.util.LastDateComputer;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class CloudFoundryApiTest {

    private static final UUID appStartedUuid = UUID.randomUUID();

    private static final UUID appStoppedUuid = UUID.randomUUID();

    private static final UUID notFoundUuid = UUID.randomUUID();

    private static final UUID errorOnGet = UUID.randomUUID();

    private static final UUID serviceInstanceUuid = UUID.randomUUID();

    private static final UUID serviceInstanceNotFoundUuid = UUID.randomUUID();


    @Mock
    private CloudFoundryClient cloudFoundryClient;

    @InjectMocks
    private CloudFoundryApi cloudFoundryApi;


    private CloudApplication sampleApplicationStarted;

    private CloudApplication sampleApplicationStopped;

    @Before
    public void setUp() {
        sampleApplicationStarted = new CloudApplication(new Meta(appStartedUuid, null, null),
                "sampleApplicationStarted");
        sampleApplicationStarted.setState(AppState.STARTED);
        sampleApplicationStopped = new CloudApplication(new Meta(appStoppedUuid, null, null),
                "sampleApplicationStopped");
        sampleApplicationStopped.setState(AppState.STOPPED);
    }

    @Test
    public void test_get_application_activity_fails_on_runtime_exception_on_get_application() {
        //given an exception will occur
        when(cloudFoundryClient.getApplication(errorOnGet))
                .thenThrow(new RuntimeException("runtime error"));
        //when get activity is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(errorOnGet), CloudFoundryException.class);
    }

    @Test
    public void test_error_thrown_when_not_found() {
        //given application is not found remotely
        when(cloudFoundryClient.getApplication(notFoundUuid))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Application not found"));
        //when get activity is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(notFoundUuid), EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.application)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(notFoundUuid.toString())));
                });
    }

    @Test
    public void test_get_application_activity_fails_on_runtime_exception_on_get_recent_log() {
        //given application is found
        when(cloudFoundryClient.getApplication(appStartedUuid)).thenReturn(sampleApplicationStarted);
        // and an exception will occur on recent log
        when(cloudFoundryClient.getRecentLogs(sampleApplicationStarted.getName()))
                .thenThrow(new RuntimeException("Call fail"));
        //when get activity is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(appStartedUuid), CloudFoundryException.class);
    }


    @Test
    public void test_get_application_activity_succeeds() throws Exception {
        final Date lastLogTime = new Date(Instant
                .now().plusSeconds(-5).getEpochSecond() * 1000);
        final Date lastEventTime = new Date(Instant
                .now().plusSeconds(-60).getEpochSecond() * 1000);
        final Date lastActionTime = lastEventTime.getTime() > lastLogTime.getTime() ? lastEventTime : lastLogTime;


        //given application is found
        when(cloudFoundryClient.getApplication(appStartedUuid)).thenReturn(sampleApplicationStarted);
        //and both recentlog and application events return data
        when(cloudFoundryClient.getRecentLogs(sampleApplicationStarted.getName()))
                .thenReturn(Arrays.asList(new ApplicationLog(sampleApplicationStarted.getName(), "",
                                new Date(lastLogTime.getTime() - 10000),
                                MessageType.STDERR, "sourceName", "sourceId"),
                        new ApplicationLog(sampleApplicationStarted.getName(), "", lastLogTime,
                                MessageType.STDERR, "sourceName", "sourceId")));
        when(cloudFoundryClient.getApplicationEvents(sampleApplicationStarted.getName())).then(
                invocationOnMock -> {
                    CloudEvent event = new CloudEvent(new Meta(UUID.randomUUID(), lastEventTime,
                            lastEventTime),
                            "someEvent");
                    event.setTimestamp(lastEventTime);
                    return Collections.singletonList(event);
                });
        //when get application activity is invoked
        ApplicationActivity applicationActivity = cloudFoundryApi.getApplicationActivity(appStartedUuid);


        //then data is returned with expected values
        assertThat(applicationActivity, notNullValue());
        assertThat(applicationActivity.getState(), is(equalTo(AppState.STARTED)));

        assertThat(applicationActivity.getLastLog(), is(equalTo(lastLogTime.toInstant())));

        assertThat(applicationActivity.getLastEvent(), is(equalTo(lastEventTime.toInstant())));

        assertThat(LastDateComputer.computeLastDate(applicationActivity.getLastLog(),
                applicationActivity.getLastEvent()), is(equalTo(lastActionTime.toInstant())));
    }

    @Test
    public void test_stop_and_start_application_fails_on_runtime_exception_on_get_application() {
        //given an exception will occur
        when(cloudFoundryClient.getApplication(errorOnGet))
                .thenThrow(new RuntimeException("runtime error"));
        //when stop or start application is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.stopApplication(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.startApplication(errorOnGet), CloudFoundryException.class);
    }

    @Test
    public void test_stop_application_fails_on_runtime_exception_on_stop_call() {
        //given application is found
        when(cloudFoundryClient.getApplication(appStartedUuid)).thenReturn(sampleApplicationStarted);
        //and stop throws an error
        doThrow(new RuntimeException("call failed")).when(cloudFoundryClient).stopApplication(any(String.class));
        //when stop or start is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.stopApplication(appStartedUuid), CloudFoundryException.class);
    }

    @Test
    public void test_sart_application_fails_on_runtime_exception_on_start_call() {
        //given application is found
        when(cloudFoundryClient.getApplication(appStoppedUuid)).thenReturn(sampleApplicationStopped);
        //and stop throws an error
        doThrow(new RuntimeException("call failed")).when(cloudFoundryClient).startApplication(any(String.class));
        //when stop or start is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.startApplication(appStoppedUuid), CloudFoundryException.class);
    }


    @Test
    public void test_stop_and_start_application_when_not_found() {
        //given application is not found remotely
        when(cloudFoundryClient.getApplication(notFoundUuid))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Application not found"));
        //when stop or start application is invoked then proper error is thrown
        verifyThrown(() -> cloudFoundryApi.stopApplication(notFoundUuid), EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.application)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(notFoundUuid.toString())));
                });
        verifyThrown(() -> cloudFoundryApi.startApplication(notFoundUuid), EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.application)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(notFoundUuid.toString())));
                });
    }

    @Test
    public void test_stop_on_stopped_application() throws Exception {
        //given get activity return a stopped application
        when(cloudFoundryClient.getApplication(appStoppedUuid)).thenReturn(sampleApplicationStopped);
        //when stop application is invoked
        cloudFoundryApi.stopApplication(appStoppedUuid);
        //then client got application
        verify(cloudFoundryClient, times(1)).getApplication(appStoppedUuid);
        //and did not asked for stop
        verify(cloudFoundryClient, never()).stopApplication(anyString());
    }

    @Test
    public void test_stop_on_started_application() throws Exception {
        //given get activity return a stopped application
        when(cloudFoundryClient.getApplication(appStartedUuid)).thenReturn(sampleApplicationStarted);
        //when stop application is invoked
        cloudFoundryApi.stopApplication(appStartedUuid);
        //then client got application
        verify(cloudFoundryClient, times(1)).getApplication(appStartedUuid);
        //and did asked for stop
        verify(cloudFoundryClient, times(1)).stopApplication(sampleApplicationStarted.getName());
    }


    @Test
    public void test_start_on_stopped_application() throws Exception {
        //given get activity return a stopped or start application
        when(cloudFoundryClient.getApplication(appStoppedUuid)).thenReturn(sampleApplicationStopped);
        //when start application is invoked
        cloudFoundryApi.startApplication(appStoppedUuid);
        //then client got application
        verify(cloudFoundryClient, times(1)).getApplication(appStoppedUuid);
        //and did asked for start
        verify(cloudFoundryClient, times(1)).startApplication(sampleApplicationStopped.getName());
    }

    @Test
    public void test_start_on_started_application() throws Exception {
        //given get activity return a stopped application
        when(cloudFoundryClient.getApplication(appStartedUuid)).thenReturn(sampleApplicationStarted);
        //when start application is invoked
        cloudFoundryApi.startApplication(appStartedUuid);
        //then client got application
        verify(cloudFoundryClient, times(1)).getApplication(appStartedUuid);
        //and did not asked for start
        verify(cloudFoundryClient, never()).startApplication(sampleApplicationStarted.getName());
    }


    @Test
    public void test_list_fails_on_runtime_exception() {
        //given list fails
        when(cloudFoundryClient.getApplications())
                .thenThrow(new RuntimeException("First call fails"));
        //when list application is invoked, proper error is thrown
        verifyThrown(() -> cloudFoundryApi.listApplications(null, null), CloudFoundryException.class);

    }


    @Test
    public void testListApplications() throws Exception {
        CloudOrganization organization = new CloudOrganization(null, "SeekAndDestroy");
        List<CloudSpace> spaces = Arrays.asList(
                new CloudSpace(new Meta(UUID.randomUUID(), new Date(), new Date()), "space-oddity", organization),
                new CloudSpace(new Meta(UUID.randomUUID(), new Date(), new Date()), "space-bound", organization)
        );
        List<Integer> applicationsIdsPerSpaces = Arrays.asList(1, 2);

        //given we have an organization, with two spaces. Each space contains two applications
        when(cloudFoundryClient.getApplications())
                .thenReturn(
                        spaces.stream().flatMap(space ->
                                        applicationsIdsPerSpaces.stream().map(cpt -> {
                                            CloudApplication application = new CloudApplication(
                                                    new Meta(UUID.randomUUID(), new Date(), new Date()),
                                                    space.getName() + "-application-" + cpt);
                                            application.setSpace(space);
                                            return application;
                                        }).collect(Collectors.toList()).stream()
                        ).collect(Collectors.toList()));
        //when list applications is invoked without filters
        List<ApplicationIdentity> applicationIdentities = cloudFoundryApi.listApplications(null, null);
        //then we get all applications
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(spaces.size() * applicationsIdsPerSpaces.size())));

        //given same conditions
        //when list applications filtering on first space
        applicationIdentities = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(), null);
        //then we get only applications of this space
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(applicationsIdsPerSpaces.size())));

        //given same conditions
        //when list applications filtering an application name pattern
        applicationIdentities = cloudFoundryApi.listApplications(null,
                Pattern.compile(".*-application-" + applicationsIdsPerSpaces.get(0)));
        //then we get only applications which name match this pattern
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(spaces.size())));

        applicationIdentities = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(),
                Pattern.compile(".*" + applicationsIdsPerSpaces.get(0)));
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(1)));

        //given same conditions
        //when list applications filtering all application name pattern and a space
        applicationIdentities = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(),
                Pattern.compile(".*"));
        //then we get a result with no entities
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(0)));
    }


    @Test
    public void test_bind_fails_on_runtime_exception_on_get_service() {
        //given get service fails
        when(cloudFoundryClient.getServices())
                .thenThrow(new RuntimeException("some error"));
        //when bind application (single or list) is invoked, proper error is thrown
        ApplicationIdentity sampleIdentity = BeanGenerator.createAppIdentity(appStartedUuid.toString());
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(sampleIdentity, serviceInstanceUuid.toString()),
                CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(Collections.singletonList(sampleIdentity),
                        serviceInstanceUuid.toString()),
                CloudFoundryException.class);

    }


    private CloudService only_one_remote_service(UUID serviceId) {
        CloudService service = new CloudService(new Meta(serviceId, new Date(), new Date()),
                "serviceInstance");
        when(cloudFoundryClient.getServices())
                .thenReturn(Collections.singletonList(service));
        return service;
    }

    @Test
    public void test_bind_fails_on_runtime_exception_ob_bind_service() {
        //given get service returns a service
        only_one_remote_service(serviceInstanceUuid);
        //and bind service fails with a runtime error
        doThrow(new RuntimeException("runtime failed")).when(cloudFoundryClient).bindService(anyString(), anyString());
        //when bind application (single or list) is invoked, proper error is thrown
        ApplicationIdentity sampleIdentity = BeanGenerator.createAppIdentity(appStartedUuid.toString());
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(sampleIdentity, serviceInstanceUuid.toString()),
                CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(Collections.singletonList(sampleIdentity),
                        serviceInstanceUuid.toString()),
                CloudFoundryException.class);
    }

    @Test
    public void test_bind_fails_on_service_not_found() {
        //given get service returns a service
        only_one_remote_service(serviceInstanceUuid);
        //when bind application (single or list) is invoked on an other servoce, proper error is thrown
        ApplicationIdentity sampleIdentity = BeanGenerator.createAppIdentity(appStartedUuid.toString());
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(sampleIdentity, serviceInstanceNotFoundUuid.toString()),
                EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.service)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(serviceInstanceNotFoundUuid.toString())));
                });
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(Collections.singletonList(sampleIdentity),
                        serviceInstanceNotFoundUuid.toString()),
                EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.service)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(serviceInstanceNotFoundUuid.toString())));
                });
    }


    @Test
    public void test_bind_succeeds() throws Exception {
        //given get service returns a service
        CloudService service = only_one_remote_service(serviceInstanceUuid);
        //when bind application (single or list) is invoked
        ApplicationIdentity sampleIdentity = BeanGenerator.createAppIdentity(appStartedUuid.toString());
        cloudFoundryApi.bindServiceInstance(sampleIdentity, serviceInstanceUuid.toString());
        List<ApplicationIdentity> applications = Arrays.asList(sampleIdentity,
                BeanGenerator.createAppIdentity(appStoppedUuid.toString()));
        cloudFoundryApi.bindServiceInstance(applications, serviceInstanceUuid.toString());
        //then bind service is invoked on each application
        verify(cloudFoundryClient, times(1 + applications.size()))
                .bindService(any(String.class), eq(service.getName()));


    }


}