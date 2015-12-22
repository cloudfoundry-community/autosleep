package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.remote.*;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException.EntityType;
import org.cloudfoundry.autosleep.util.BeanGenerator;
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
public class ApplicationStopperTest {

    private static final UUID APP_UID = UUID.fromString("9AF63B10-9D25-4162-9AD2-5AA8173FFC3B");

    private static final String INSTANCE_ID = "ACTestSId";

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

    @Mock
    private ApplicationLocker applicationLocker;

    private ApplicationStopper spyChecker;


    /**
     * Build mocks.
     */
    @Before
    public void buildMocks() throws EntityNotFoundException, CloudFoundryException {
        //default

        when(application.getGuid()).thenReturn(APP_UID.toString());
        when(application.getName()).thenReturn("appName");
        when(applicationActivity.getApplication()).thenReturn(application);



        applicationInfo = spy(BeanGenerator.createAppInfo(APP_UID.toString(), INSTANCE_ID)
                .withRemoteInfo(applicationActivity));

        when(cloudFoundryApi.getApplicationActivity(APP_UID)).thenReturn(applicationActivity);

        when(applicationRepository.findOne(APP_UID.toString())).thenReturn(
                applicationInfo
        );

        doAnswer(invocationOnMock -> {
            ((Runnable)invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));


        spyChecker = spy(ApplicationStopper.builder()
                .appUid(APP_UID)
                .serviceInstanceId(INSTANCE_ID)
                .bindingId(BINDING_ID)
                .period(INTERVAL)
                .cloudFoundryApi(cloudFoundryApi)
                .clock(clock)
                .applicationLocker(applicationLocker)
                .applicationRepository(applicationRepository).build());
    }


    @Test
    public void test_start_execute_task() throws Exception {
        doAnswer(invocationOnMock -> {
            log.debug("Fake scheduling");
            spyChecker.run();
            return null;
        }).when(clock).scheduleTask(eq(BINDING_ID), eq(Duration.ofSeconds(0)), eq(spyChecker));
        spyChecker.startNow();

        verify(clock, times(1)).scheduleTask(BINDING_ID, Duration.ofSeconds(0), spyChecker);
        verify(spyChecker, times(1)).run();
        // and application is saved at the end
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void application_is_not_stopped_when_active() throws Exception {
        //given the application is started and active
        when(applicationActivity.getState()).thenReturn(AppState.STARTED);
        when(applicationActivity.getLastEvent()).thenReturn(Instant.now());
        when(applicationActivity.getLastLog()).thenReturn(Instant.now());
        //when task is run
        spyChecker.run();

        //then it see the application as monitored
        verify(spyChecker, times(1)).handleApplicationMonitored(applicationInfo);
        //and it never stopped the application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and it schedules task on default period
        verify(clock, times(1)).scheduleTask(any(), anyObject(), any());
        verify(spyChecker, never()).rescheduleWithDefaultPeriod();
        // and application is saved at the end
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));

    }

    @Test
    public void application_is_stopped_when_inactive() throws Exception {
        //given the application is started but not active
        when(applicationActivity.getState()).thenReturn(AppState.STARTED);
        when(applicationActivity.getLastEvent()).thenReturn(Instant.now().minus(INTERVAL.multipliedBy(2)));
        when(applicationActivity.getLastLog()).thenReturn(Instant.now().minus(INTERVAL.multipliedBy(2)));
        //when task is run
        spyChecker.run();
        //then it see the application as monitored
        verify(spyChecker, times(1)).handleApplicationMonitored(applicationInfo);
        //and it did stop the application
        verify(cloudFoundryApi, times(1)).stopApplication(APP_UID);
        verify(applicationInfo, times(1)).markAsPutToSleep();
        //and it schedules task on default period
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        // and application is saved at the end
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void application_is_not_stopped_if_already_stopped() throws Exception {
        //given the application is stopped
        when(applicationActivity.getState()).thenReturn(CloudApplication.AppState.STOPPED);
        //when task is run
        spyChecker.run();
        //then it see the application as monitored
        verify(spyChecker, times(1)).handleApplicationMonitored(applicationInfo);
        //and it never stopped the application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and it schedules task on default period
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        // and application is saved at the end
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void task_is_reschedule_even_when_remote_error() throws Exception {
        //given remote call fails for some reason
        when(cloudFoundryApi.getApplicationActivity(APP_UID))
                .thenThrow(new CloudFoundryException(new Exception("Mock call")));
        //when task is run
        spyChecker.run();
        //then it never stopped application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and task is rescheduled
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        //and application is saved
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void task_is_reschedule_even_when_not_found_remotely() throws Exception {
        //given remote application is not found
        when(cloudFoundryApi.getApplicationActivity(APP_UID))
                .thenThrow(new EntityNotFoundException(EntityType.application, APP_UID.toString()));
        //when task is run
        spyChecker.run();
        //then it never stopped application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and task is rescheduled
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        //and application is saved
        verify(spyChecker, times(1)).handleApplicationMonitored(applicationInfo);
    }

    @Test
    public void application_is_not_stopped_if_no_activity_found() throws Exception {
        //given remote activity is not found
        when(applicationActivity.getLastLog()).thenReturn(null);
        when(applicationActivity.getLastEvent()).thenReturn(null);
        //when task is run
        spyChecker.run();
        //then it see the application as monitored
        verify(spyChecker, times(1)).handleApplicationMonitored(applicationInfo);
        //and it never called stop application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and it rescheduled with default period
        verify(spyChecker, times(1)).rescheduleWithDefaultPeriod();
        //and application is saved
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }

    @Test
    public void task_is_removed_when_application_not_watched_by_service() throws Exception {
        //given application is marked as ignored
        applicationInfo.getEnrollmentState().updateEnrollment(INSTANCE_ID, false);
        //when task is run
        spyChecker.run();
        //then it sees it as an ignored application
        verify(spyChecker, times(1)).handleApplicationIgnored(applicationInfo);
        //and it never stops application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and clears information
        verify(applicationInfo, times(1)).clearCheckInformation();
        //and it never reschedules task
        verify(spyChecker, never()).rescheduleWithDefaultPeriod();
        //and it removes task from known tasks
        verify(clock, times(1)).removeTask(BINDING_ID);
        //and it saves application current information
        verify(applicationRepository, times(1)).save(any(ApplicationInfo.class));
    }


    @Test
    public void task_is_removed_when_not_found_localy() throws Exception {
        //given application is not found locally
        when(applicationRepository.findOne(APP_UID.toString()))
                .thenReturn(null);
        //when task is run
        spyChecker.run();
        //then it handles application as not found
        verify(spyChecker, times(1)).handleApplicationNotFound();
        //and it never reschedules task
        verify(clock, never()).scheduleTask(anyObject(), anyObject(), anyObject());
        //and never stops application
        verify(cloudFoundryApi, never()).stopApplication(APP_UID);
        //and removes the task from known tasks
        verify(clock, times(1)).removeTask(BINDING_ID);

    }


}