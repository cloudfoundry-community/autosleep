package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ASServiceBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.client.lib.domain.CloudApplication;
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
    private CloudFoundryApiService mockRemote;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private BindingRepository bindingRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    private AppStateChecker spyChecker;


    /**
     * Build mocks.
     */
    @Before
    public void buildMocks() {
        when(mockRemote.getApplicationInfo(APP_UID)).thenReturn(applicationInfo);
        spyChecker = spy(new AppStateChecker(APP_UID, BINDING_ID, INTERVAL, mockRemote, clock, bindingRepository,
                applicationRepository));

    }


    @Test
    public void testStart() throws Exception {
        doAnswer(invocationOnMock -> {
            spyChecker.run();
            return null;
        }).when(clock).scheduleTask(BINDING_ID, Duration.ofSeconds(0), spyChecker);

        when(applicationInfo.computeLastDate()).thenReturn(Instant.now());

        when(bindingRepository.findOne(BINDING_ID)).thenReturn(new ASServiceBinding(BINDING_ID,
                "serviceInstance", null, null, APP_UID.toString()));

        spyChecker.start();
        verify(clock, times(1)).scheduleTask(BINDING_ID, Duration.ofSeconds(0), spyChecker);
        verify(spyChecker, times(1)).run();


    }

    @Test
    public void testRunOnActive() throws Exception {
        when(applicationInfo.computeLastDate()).thenReturn(Instant.now());
        when(bindingRepository.findOne(BINDING_ID)).thenReturn(new ASServiceBinding(BINDING_ID,
                "serviceInstance",
                null, null, APP_UID.toString()));
        spyChecker.run();
        verify(mockRemote, never()).stopApplication(APP_UID);
        verify(clock, times(1)).scheduleTask(any(), anyObject(), any());
        verify(spyChecker, never()).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testRunOnInactive() throws Exception {
        when(applicationInfo.computeLastDate()).thenReturn(Instant.now().minus(INTERVAL.multipliedBy(2)));
        when(bindingRepository.findOne(BINDING_ID)).thenReturn(new ASServiceBinding(BINDING_ID,
                "serviceInstance",
                null, null, APP_UID.toString()));
        spyChecker.run();
        verify(mockRemote, times(1)).stopApplication(APP_UID);
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testRunOnAlreadyStopped() throws Exception {
        when(applicationInfo.getState()).thenReturn(CloudApplication.AppState.STOPPED);
        when(bindingRepository.findOne(BINDING_ID)).thenReturn(new ASServiceBinding(BINDING_ID,
                "serviceInstance",
                null, null, APP_UID.toString()));
        spyChecker.run();
        verify(mockRemote, never()).stopApplication(APP_UID);
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testStopByItself() throws Exception {

        when(bindingRepository.findOne(BINDING_ID))
                .thenReturn(new ASServiceBinding(BINDING_ID, "serviceInstance", null, null, APP_UID.toString()))
                .thenReturn(null);

        doAnswer(invocationOnMock -> {
            log.debug("Fake scheduling");
            spyChecker.run();
            return null;
        }).doAnswer(invocationOnMock -> {
            log.debug("Fake rescheduling");
            Thread.sleep(INTERVAL.toMillis());
            log.debug("Fake rerunning");
            spyChecker.run();
            return null;
        }).when(clock).scheduleTask(eq(BINDING_ID), anyObject(), anyObject());


        when(applicationInfo.computeLastDate()).thenReturn(Instant.now());

        spyChecker.start();
        Thread.sleep(INTERVAL.multipliedBy(3).toMillis());
        //start schedule one time, that called run and then an other schedule, an other run and remove
        verify(clock, times(2)).scheduleTask(anyObject(), anyObject(), anyObject());
        verify(spyChecker, times(2)).run();
        verify(clock, times(1)).removeTask(BINDING_ID);

    }

}