package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.IRemote;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {Clock.class})
public class AppStateCheckerTest {

    private static final String APP_UID = "987987";
    private static final String TASK_ID = "DP98USD";
    private static final Duration INTERVAL = Duration.ofMillis(300);

    @Autowired
    protected Clock clock;

    private IRemote mockRemote;
    private ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
    private AppStateChecker checker;
    private AppStateChecker spyChecker;


    @Before
    public void buildMocks() {
        mockRemote = mock(IRemote.class);
        when(mockRemote.getApplicationInfo(APP_UID)).thenReturn(applicationInfo);
        checker = new AppStateChecker(APP_UID, TASK_ID, INTERVAL, mockRemote,clock);
        spyChecker = spy(checker);
    }

    /** Test with one date, no activity between checker start and next verification. */
    @Test
    public void testWithIdleApp() throws Exception {
        log.debug("Start testWithIdleApp()");
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spyChecker.start();
        Thread.sleep(INTERVAL.toMillis()+INTERVAL.toMillis()/5);
        verify(spyChecker,times(2)).run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
    }

    @Test
    public void testWithOldApp() throws Exception {
        log.debug("Start testWithOldApp()");
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now().minus(Duration.ofDays(1)));
        spyChecker.start();
        Thread.sleep(100);
        //verify that checker stops instantly
        verify(spyChecker,times(1)).run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
    }

    @Test
    public void testWithActiveApp() throws Exception {
        log.debug("Start testWithActiveApp()");
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spyChecker.start();
        Thread.sleep(INTERVAL.toMillis() / 2);
        //simulate activty before the end of the interval
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        Thread.sleep(INTERVAL.toMillis() * 2 );
        verify(spyChecker,times(3)).run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
    }

    @Test
    public void testStop() throws Exception {
        log.debug("Start testStop()");
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spyChecker.start();
        Thread.sleep(INTERVAL.toMillis() / 2 );
        spyChecker.stop();

        verify(mockRemote, never()).stopApplication(APP_UID);
        verify(spyChecker, times(1)).run();
        verify(spyChecker, times(1)).start();
        verify(spyChecker, times(1)).stop();
        //verifyNoMoreInteractions(spyChecker);
        Thread.sleep(INTERVAL.toMillis() * 2 );
        verifyNoMoreInteractions(spyChecker);


    }

}