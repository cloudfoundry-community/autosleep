package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.Invocation;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class CloudFoundryApiTest {

    private static class LastCallVerification implements VerificationMode {
        @Override
        public void verify(VerificationData verificationData) {
            //verify that the last called is the method wanted
            List<Invocation> invocations = verificationData.getAllInvocations();
            InvocationMatcher matcher = verificationData.getWanted();
            Invocation invocation = invocations.get(invocations.size() - 1);
            if (!matcher.matches(invocation)) {
                throw new MockitoException("Verification failed: " + matcher.toString() + " is not the last called");
            }
        }
    }

    private static final UUID appStartedUuid = UUID.randomUUID();

    private static final UUID appStoppedUuid = UUID.randomUUID();

    private static final UUID notFoundUuid = UUID.randomUUID();

    private static final UUID errorUuid = UUID.randomUUID();

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
        when(cloudFoundryClient.getApplication(notFoundUuid)).thenReturn(null);
        when(cloudFoundryClient.getApplication(errorUuid)).thenThrow(new RuntimeException("runtime error"));
    }

    @Test
    public void testGetApplicationInfo() throws Exception {
        assertThat(cloudFoundryApi.getApplicationInfo(errorUuid), is(nullValue()));
        assertThat(cloudFoundryApi.getApplicationInfo(notFoundUuid), is(nullValue()));


        Date lastLogTime = new Date(Instant
                .now().plusSeconds(-5).getEpochSecond() * 1000);
        Date lastEventTime = new Date(Instant
                .now().plusSeconds(-60).getEpochSecond() * 1000);
        Date lastActionTime = lastEventTime.getTime() > lastLogTime.getTime() ? lastEventTime : lastLogTime;

        log.debug("lastLogTime = {}, lastEventTime={}, lastActionTime={}",
                lastLogTime, lastEventTime, lastActionTime);

        when(cloudFoundryClient.getRecentLogs(sampleApplicationStarted.getName()))
                .thenReturn(Arrays.asList(new ApplicationLog(sampleApplicationStarted.getName(), "",
                                new Date(lastLogTime.getTime() - 10000),
                                MessageType.STDERR, "sourceName", "sourceId"),
                        new ApplicationLog(sampleApplicationStarted.getName(), "", lastLogTime,
                                MessageType.STDERR, "sourceName", "sourceId")));
        UUID randomUuid = UUID.randomUUID();
        when(cloudFoundryClient.getApplicationEvents(sampleApplicationStarted.getName())).then(
                invocationOnMock -> {
                    CloudEvent event = new CloudEvent(new Meta(randomUuid, lastEventTime,
                            lastEventTime),
                            "someEvent");
                    event.setTimestamp(lastEventTime);
                    return Collections.singletonList(event);
                });

        ApplicationInfo applicationInfo = cloudFoundryApi.getApplicationInfo(appStartedUuid);
        assertThat(applicationInfo, notNullValue());
        assertThat(applicationInfo.getState(), is(equalTo(AppState.STARTED)));

        assertThat(applicationInfo.getLastLog(), is(equalTo(lastLogTime.toInstant())));

        assertThat(applicationInfo.getLastEvent(), is(equalTo(lastEventTime.toInstant())));

        assertThat(applicationInfo.computeLastDate(), is(equalTo(lastActionTime.toInstant())));
    }

    @Test
    public void testStopApplication() throws Exception {
        log.debug("testStopApplication - stopping application started");
        cloudFoundryApi.stopApplication(appStartedUuid);
        cloudFoundryApi.stopApplication(appStoppedUuid);
        cloudFoundryApi.stopApplication(errorUuid);
        cloudFoundryApi.stopApplication(notFoundUuid);
        verify(cloudFoundryClient, times(4)).getApplication(any(UUID.class));
        verify(cloudFoundryClient, times(1)).stopApplication(sampleApplicationStarted.getName());
        verify(cloudFoundryClient, never()).stopApplication(sampleApplicationStopped.getName());
        verify(cloudFoundryClient, new LastCallVerification()).getApplication(notFoundUuid);
    }

    @Test
    public void testStartApplication() throws Exception {
        cloudFoundryApi.startApplication(appStartedUuid);
        cloudFoundryApi.startApplication(appStoppedUuid);
        cloudFoundryApi.startApplication(errorUuid);
        cloudFoundryApi.startApplication(notFoundUuid);
        verify(cloudFoundryClient, times(4)).getApplication(any(UUID.class));
        verify(cloudFoundryClient, times(1)).startApplication(sampleApplicationStopped.getName());
        verify(cloudFoundryClient, never()).startApplication(sampleApplicationStarted.getName());
        verify(cloudFoundryClient, new LastCallVerification()).getApplication(notFoundUuid);
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
        List<UUID> remoteApplications = cloudFoundryApi.listApplications(null, null);
        assertThat(remoteApplications, is(nullValue()));

        remoteApplications = cloudFoundryApi.listApplications(null, null);
        assertThat(remoteApplications, is(notNullValue()));
        assertThat(remoteApplications.size(), is(equalTo(spaces.size() * applicationsIdsPerSpaces.size())));

        remoteApplications = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(), null);
        assertThat(remoteApplications, is(notNullValue()));
        assertThat(remoteApplications.size(), is(equalTo(applicationsIdsPerSpaces.size())));

        remoteApplications = cloudFoundryApi.listApplications(null,
                ".*-application-" + applicationsIdsPerSpaces.get(0));
        assertThat(remoteApplications, is(notNullValue()));
        assertThat(remoteApplications.size(), is(equalTo(spaces.size())));

        remoteApplications = cloudFoundryApi.listApplications(spaces.get(0).getMeta().getGuid(),
                ".*-application-" + applicationsIdsPerSpaces.get(0));
        assertThat(remoteApplications, is(notNullValue()));
        assertThat(remoteApplications.size(), is(equalTo(1)));

    }

    @Test
    public void testRuntimeExceptions() throws Exception {
        when(cloudFoundryClient.getApplication(appStartedUuid)).thenThrow(new RuntimeException("TestRuntimeException"));
        cloudFoundryApi.startApplication(appStartedUuid);
        cloudFoundryApi.stopApplication(appStartedUuid);
        cloudFoundryApi.getApplicationInfo(appStartedUuid);
    }
}