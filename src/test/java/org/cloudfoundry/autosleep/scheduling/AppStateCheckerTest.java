package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationInfo;
import org.cloudfoundry.autosleep.remote.IRemote;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@ContextConfiguration(classes = {Clock.class})
public class AppStateCheckerTest {

    private static final String APP_UID = "987987";
    private static final String TASK_ID = "DP98USD";
    private static final Duration INTERVAL = Duration.ofMillis(150);

    @Autowired
    protected Clock clock;

    private IRemote mockRemote = mock(IRemote.class);
    private ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
    private AppStateChecker checker;



    @PostConstruct
    protected void init() {
        checker = new AppStateChecker(APP_UID, TASK_ID, INTERVAL, mockRemote,clock);
        when(mockRemote.getApplicationInfo(APP_UID)).thenReturn(applicationInfo);
    }

    /** Test with one date, no activity between checker start and next verification. */
    @Test
    public void testWithIdleApp() throws Exception {
        AppStateChecker spy = spy(checker);
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spy.start();
        Thread.sleep(INTERVAL.toMillis() + INTERVAL.toMillis() / 10);
        verify(spy,times(2)).run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
    }

    @Test
    public void testWithOldApp() throws Exception {
        AppStateChecker spy = spy(checker);
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now().minus(Duration.ofDays(1)));
        spy.start();
        Thread.sleep(100);
        //verify that checker stops instantly
        verify(spy,times(1)).run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
    }

    @Test
    public void testWithActiveApp() throws Exception {
        AppStateChecker spy = spy(checker);
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spy.start();
        Thread.sleep(INTERVAL.toMillis() / 2);
        //simulate activty before the end of the interval
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        Thread.sleep(INTERVAL.toMillis() * 2 );
        verify(spy,times(3)).run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
    }


    @Test
    public void testStop() throws Exception {
        AppStateChecker spy = spy(checker);
        when(applicationInfo.getLastDeployed()).thenReturn(LocalDateTime.now());
        spy.start();
        Thread.sleep(INTERVAL.toMillis() / 2 );
        spy.stop();

        verify(mockRemote, never()).stopApplication(APP_UID);
        reset(spy);
        Thread.sleep(INTERVAL.toMillis() * 2 );
        verify(spy, never()).run();

    }

}