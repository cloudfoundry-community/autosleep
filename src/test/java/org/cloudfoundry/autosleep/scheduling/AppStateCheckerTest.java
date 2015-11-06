package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;


@Slf4j
public class AppStateCheckerTest {

    private static final UUID APP_UID = UUID.fromString("9AF63B10-9D25-4162-9AD2-5AA8173FFC3B");
    private static final String TASK_ID = "DP98USD";
    private static final Duration INTERVAL = Duration.ofMillis(300);

    private Clock clock;
    private CloudFoundryApiService mockRemote;
    private ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
    private AppStateChecker spyChecker;


    /** Build mocks. */
    @Before
    public void buildMocks() {
        mockRemote = mock(CloudFoundryApiService.class);
        clock = mock(Clock.class);
        when(mockRemote.getApplicationInfo(APP_UID)).thenReturn(applicationInfo);
        spyChecker = spy(new AppStateChecker(APP_UID, TASK_ID, INTERVAL, mockRemote,clock));
    }

    @Test
    public void testStart() throws Exception {
        spyChecker.start();
        verify(clock,times(1)).scheduleTask(TASK_ID,Duration.ofSeconds(0),spyChecker);

    }

    @Test
    public void testRunOnActive() throws Exception {
        when(applicationInfo.getLastActionDate()).thenReturn(LocalDateTime.now());
        spyChecker.run();
        verify(mockRemote, never()).stopApplication(APP_UID);
        verify(clock,times(1)).scheduleTask(any(),anyObject(),any());
        verify(spyChecker,never()).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testRunOnInactive() throws Exception {
        when(applicationInfo.getLastActionDate()).thenReturn(LocalDateTime.now().minus(INTERVAL.multipliedBy(2)));
        spyChecker.run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
        verify(spyChecker,times(1)).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testRunOnAlreadyStopped() throws Exception {
        when(applicationInfo.getState()).thenReturn(CloudApplication.AppState.STOPPED);
        spyChecker.run();
        verify(mockRemote, never()).stopApplication(APP_UID);
        verify(spyChecker,times(1)).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testStop() throws Exception {
        spyChecker.stop();
        verify(clock,never()).scheduleTask(any(), anyObject(), any());
        verify(clock, times(1)).stopTimer(TASK_ID);
    }

}