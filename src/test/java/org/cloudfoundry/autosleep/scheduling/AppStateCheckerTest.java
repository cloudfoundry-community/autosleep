package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.IRemote;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;


@Slf4j
public class AppStateCheckerTest {

    private static final String APP_UID = "987987";
    private static final String TASK_ID = "DP98USD";
    private static final Duration INTERVAL = Duration.ofMillis(300);

    private Clock clock;
    private IRemote mockRemote;
    private ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
    private AppStateChecker spyChecker;


    /** Build mocks. */
    @Before
    public void buildMocks() {
        mockRemote = mock(IRemote.class);
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
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spyChecker.run();
        verify(mockRemote, never()).stopApplication(APP_UID);
        verify(clock,times(1)).scheduleTask(any(),anyObject(),any());
    }

    @Test
    public void testRunOnInactive() throws Exception {
        log.debug("Start testWithActiveApp()");
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now().minus(INTERVAL.multipliedBy(2)));
        spyChecker.run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
        verify(clock,never()).scheduleTask(any(), anyObject(), any());
    }

    @Test
    public void testStop() throws Exception {
        spyChecker.stop();
        verify(clock,never()).scheduleTask(any(), anyObject(), any());
        verify(clock, times(1)).stopTimer(TASK_ID);
    }

}