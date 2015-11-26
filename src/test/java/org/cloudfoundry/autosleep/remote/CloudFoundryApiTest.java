package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException.EntityType;
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

    private static final UUID serviceInstanceErrorUuid = UUID.randomUUID();

    @Mock
    private CloudFoundryClient cloudFoundryClient;

    @InjectMocks
    private CloudFoundryApi cloudFoundryApi;


    private CloudApplication sampleApplicationStarted;

    private CloudApplication sampleApplicationStopped;

    @Before
    public void setUp() {
        sampleApplicationStarted = new CloudApplication("sampleApplicationStarted", "commandUrl", "buildPackUrl",
                1024, 1, Collections.singletonList("accessUri"),
                Arrays.asList("service1", "service2"), AppState.STARTED);
        sampleApplicationStopped = new CloudApplication("sampleApplicationStopped", "commandUrl", "buildPackUrl",
                1024, 1, Collections.singletonList("accessUri"),
                Arrays.asList("service1", "service2"), AppState.STOPPED);

        when(cloudFoundryClient.getApplication(appStartedUuid)).thenReturn(sampleApplicationStarted);
        when(cloudFoundryClient.getApplication(appStoppedUuid)).thenReturn(sampleApplicationStopped);
        when(cloudFoundryClient.getApplication(notFoundUuid))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Application not found"));
        when(cloudFoundryClient.getApplication(errorOnGet))
                .thenThrow(new RuntimeException("runtime error"))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Remote error") );


    }


    @Test
    public void testGetApplicationActivity() throws Exception {
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(notFoundUuid), EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.application)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(notFoundUuid.toString())));
                });


        Date lastLogTime = new Date(Instant
                .now().plusSeconds(-5).getEpochSecond() * 1000);
        Date lastEventTime = new Date(Instant
                .now().plusSeconds(-60).getEpochSecond() * 1000);
        Date lastActionTime = lastEventTime.getTime() > lastLogTime.getTime() ? lastEventTime : lastLogTime;

        log.debug("lastLogTime = {}, lastEventTime={}, lastActionTime={}",
                lastLogTime, lastEventTime, lastActionTime);

        when(cloudFoundryClient.getRecentLogs(sampleApplicationStarted.getName()))
                .thenThrow(new RuntimeException("First call failsr"))
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
        verifyThrown(() -> cloudFoundryApi.getApplicationActivity(appStartedUuid), CloudFoundryException.class);
        ApplicationActivity applicationActivity = cloudFoundryApi.getApplicationActivity(appStartedUuid);
        assertThat(applicationActivity, notNullValue());
        assertThat(applicationActivity.getState(), is(equalTo(AppState.STARTED)));

        assertThat(applicationActivity.getLastLog(), is(equalTo(lastLogTime.toInstant())));

        assertThat(applicationActivity.getLastEvent(), is(equalTo(lastEventTime.toInstant())));

        assertThat(LastDateComputer.computeLastDate(applicationActivity.getLastLog(),
                applicationActivity.getLastEvent()), is(equalTo(lastActionTime.toInstant())));
    }

    @Test
    public void testStopApplication() throws Exception {
        verifyThrown(() -> cloudFoundryApi.stopApplication(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.stopApplication(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.stopApplication(notFoundUuid), EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.application)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(notFoundUuid.toString())));
                });

        cloudFoundryApi.stopApplication(appStartedUuid);
        cloudFoundryApi.stopApplication(appStoppedUuid);

        verify(cloudFoundryClient, times(5)).getApplication(any(UUID.class));
        verify(cloudFoundryClient, times(1)).stopApplication(sampleApplicationStarted.getName());
        verify(cloudFoundryClient, never()).stopApplication(sampleApplicationStopped.getName());
        doThrow(new RuntimeException("call failed")).when(cloudFoundryClient).stopApplication(any(String.class));
        verifyThrown(() -> cloudFoundryApi.stopApplication(appStartedUuid), CloudFoundryException.class);
    }


    @Test
    public void testStartApplication() throws Exception {
        verifyThrown(() -> cloudFoundryApi.startApplication(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.startApplication(errorOnGet), CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.startApplication(notFoundUuid), EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.application)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(notFoundUuid.toString())));
                });
        cloudFoundryApi.startApplication(appStartedUuid);
        cloudFoundryApi.startApplication(appStoppedUuid);
        verify(cloudFoundryClient, times(5)).getApplication(any(UUID.class));
        verify(cloudFoundryClient, times(1)).startApplication(sampleApplicationStopped.getName());
        verify(cloudFoundryClient, never()).startApplication(sampleApplicationStarted.getName());
        doThrow(new RuntimeException("call failed")).when(cloudFoundryClient).startApplication(any(String.class));
        verifyThrown(() -> cloudFoundryApi.startApplication(appStoppedUuid), CloudFoundryException.class);

    }


    @Test
    public void testListApplications() throws Exception {
        CloudOrganization organization = new CloudOrganization(null, "SeekAndDestroy");
        List<CloudSpace> spaces = Arrays.asList(
                new CloudSpace(new Meta(UUID.randomUUID(), new Date(), new Date()), "space-oddity", organization),
                new CloudSpace(new Meta(UUID.randomUUID(), new Date(), new Date()), "space-bound", organization)
        );
        List<Integer> applicationsIdsPerSpaces = Arrays.asList(1, 2);

        when(cloudFoundryClient.getApplications())
                .thenThrow(new RuntimeException("First call fails"))
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


        //test that remote call throws an exception
        verifyThrown(() -> cloudFoundryApi.listApplications(null, null), CloudFoundryException.class);

        List<ApplicationIdentity> applicationIdentities = cloudFoundryApi.listApplications(null, null);
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(spaces.size() * applicationsIdsPerSpaces.size())));

        applicationIdentities = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(), null);
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(applicationsIdsPerSpaces.size())));

        applicationIdentities = cloudFoundryApi.listApplications(null,
                Pattern.compile(".*-application-" + applicationsIdsPerSpaces.get(0)));
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(spaces.size())));

        applicationIdentities = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(),
                Pattern.compile(".*" + applicationsIdsPerSpaces.get(0)));
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(1)));

        applicationIdentities = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(),
                Pattern.compile(".*"));
        assertThat(applicationIdentities, is(notNullValue()));
        assertThat(applicationIdentities.size(), is(equalTo(0)));
    }


    @Test
    public void testBindServiceInstance() throws Exception {
        ApplicationIdentity applicationStarted = new ApplicationIdentity(appStartedUuid, "applicationStarted");
        List<ApplicationIdentity> applications = Arrays.asList(applicationStarted,
                new ApplicationIdentity(appStoppedUuid, "applicationStopped"));
        CloudService service = new CloudService(new Meta(serviceInstanceUuid, new Date(), new Date()),
                "serviceInstance");
        when(cloudFoundryClient.getServices())
                .thenThrow(new RuntimeException("some error"))
                .thenThrow(new RuntimeException("some error"))
                .thenReturn(Collections.singletonList(service));


        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(applicationStarted, serviceInstanceUuid.toString()),
                CloudFoundryException.class);
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(applications, serviceInstanceUuid.toString()),
                CloudFoundryException.class);

        verifyThrown(() -> cloudFoundryApi
                        .bindServiceInstance(applicationStarted, serviceInstanceNotFoundUuid.toString()),
                EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.service)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(serviceInstanceNotFoundUuid.toString())));
                });
        verifyThrown(() -> cloudFoundryApi
                        .bindServiceInstance(applications, serviceInstanceNotFoundUuid.toString()),
                EntityNotFoundException.class,
                exceptionThrown -> {
                    assertThat(exceptionThrown.getEntityType(), is(equalTo(EntityType.service)));
                    assertThat(exceptionThrown.getEntityId(), is(equalTo(serviceInstanceNotFoundUuid.toString())));
                });


        cloudFoundryApi.bindServiceInstance(applicationStarted, serviceInstanceUuid.toString());
        verify(cloudFoundryClient, times(1)).bindService(eq(applicationStarted.getName()), eq(service.getName()));
        cloudFoundryApi.bindServiceInstance(applications, serviceInstanceUuid.toString());
        verify(cloudFoundryClient, times(1 + applications.size()))
                .bindService(any(String.class), eq(service.getName()));

        doThrow(new RuntimeException("runtime failed")).when(cloudFoundryClient).bindService(anyString(), anyString());
        verifyThrown(() -> cloudFoundryApi.bindServiceInstance(applicationStarted, serviceInstanceUuid.toString()),
                CloudFoundryException.class);

    }


}