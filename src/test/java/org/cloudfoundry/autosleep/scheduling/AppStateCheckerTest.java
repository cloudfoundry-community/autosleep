package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.autosleep.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException.EntityType;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class AppStateCheckerTest {

    private static final UUID APP_UID = UUID.fromString("9AF63B10-9D25-4162-9AD2-5AA8173FFC3B");

    private static final String BINDING_ID = "DP98USD";

    private static final Duration INTERVAL = Duration.ofMillis(300);

    @Mock
    private Clock clock;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private ApplicationIdentity application;

    @Mock
    private ApplicationActivity applicationActivity;

    private ApplicationInfo applicationInfo;

    @Mock
    private ApplicationRepository applicationRepository;

    private AppStateChecker spyChecker;


    /**
     * Build mocks.
     */
    @Before
    public void buildMocks() throws EntityNotFoundException, CloudFoundryException {
        //default
        when(application.getGuid()).thenReturn(APP_UID);
        when(application.getName()).thenReturn("appName");
        when(applicationActivity.getApplication()).thenReturn(application);
        when(applicationActivity.getLastEvent()).thenReturn(Instant.now());
        when(applicationActivity.getLastLog()).thenReturn(Instant.now());
        when(applicationActivity.getState()).thenReturn(AppState.STARTED);

        applicationInfo = spy(new ApplicationInfo(APP_UID, "ACTestSId").withRemoteInfo(applicationActivity));

        when(cloudFoundryApi.getApplicationActivity(APP_UID)).thenReturn(applicationActivity);

        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(
                applicationInfo
        );


        spyChecker = spy(AppStateChecker.builder()
                .appUid(APP_UID)
                .bindingId(BINDING_ID)
                .period(INTERVAL)
                .cloudFoundryApi(cloudFoundryApi)
                .clock(clock)
                .applicationRepository(applicationRepository).build());
    }


    @Test
    public void testStart() throws Exception {
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        doAnswer(invocationOnMock -> {
            log.debug("Fake scheduling");
            spyChecker.run();
            return null;
        }).when(clock).scheduleTask(eq(BINDING_ID), eq(Duration.ofSeconds(0)), eq(spyChecker));
        spyChecker.startNow();

        verify(clock, times(1)).scheduleTask(BINDING_ID, Duration.ofSeconds(0), spyChecker);
        verify(spyChecker, times(1)).run();
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void testRunOnActive() throws Exception {
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        spyChecker.run();
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(clock, times(1)).scheduleTask(any(), anyObject(), any());
        verify(spyChecker, never()).rescheduleWithDefaultPeriod();
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void testRunOnInactive() throws Exception {
        when(applicationActivity.getLastEvent()).thenReturn(Instant.now().minus(INTERVAL.multipliedBy(2)));
        when(applicationActivity.getLastLog()).thenReturn(Instant.now().minus(INTERVAL.multipliedBy(2)));
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        spyChecker.run();
        verify(cloudFoundryApi, times(1)).stopApplication(APP_UID);
        verify(applicationInfo, times(1)).markAsPutToSleep();
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void testRunOnAlreadyStopped() throws Exception {
        when(applicationActivity.getState()).thenReturn(CloudApplication.AppState.STOPPED);
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        spyChecker.run();
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void testRunOnActivityNotFound() throws Exception {
        when(cloudFoundryApi.getApplicationActivity(APP_UID))
                .thenThrow(new CloudFoundryException(new Exception("Mock call")))
                .thenThrow(new EntityNotFoundException(EntityType.application, APP_UID.toString()));
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        spyChecker.run();
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
        spyChecker.run();
        verify(spyChecker, times(2)).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testRunOnLastEventNotFound() throws Exception {
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        when(applicationActivity.getLastLog()).thenReturn(null);
        when(applicationActivity.getLastEvent()).thenReturn(null);
        spyChecker.run();
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void testRunOnOptedOut() throws Exception {
        applicationInfo.getStateMachine().onOptOut();
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(applicationInfo);
        spyChecker.run();
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(applicationInfo, times(1)).clearCheckInformation();
        verify(spyChecker, never()).rescheduleWithDefaultPeriod();
        verify(clock, times(1)).removeTask(BINDING_ID);
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }


    @Test
    public void tesStopIfBindingRemoved() throws Exception {
        when(applicationRepository.findOne(APP_UID.toString()))
                .thenReturn(null);
        spyChecker.run();
        verify(clock, never()).scheduleTask(anyObject(), anyObject(), anyObject());
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(clock, times(1)).removeTask(BINDING_ID);
    }

    @Test
    public void tesStopIfApplicationRemoved() throws Exception {
        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(null);
        spyChecker.run();
        verify(clock, never()).scheduleTask(anyObject(), anyObject(), anyObject());
        verify(spyChecker, times(1)).run();
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        verify(clock, times(1)).removeTask(BINDING_ID);

    }

}