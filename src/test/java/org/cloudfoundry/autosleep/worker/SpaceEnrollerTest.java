package org.cloudfoundry.autosleep.worker;

import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.worker.remote.EntityNotFoundException;
import org.cloudfoundry.autosleep.worker.remote.EntityNotFoundException.EntityType;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpaceEnrollerTest {

    private static final String APP_ID = UUID.randomUUID().toString();

    private static final Duration INTERVAL = Duration.ofMillis(300);

    private static final String SERVICE_ID = "serviceId";

    private static final String SPACE_ID = UUID.randomUUID().toString();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private Clock clock;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private DeployedApplicationConfig.Deployment deployment;

    private List<String> remoteApplicationIds = Arrays.asList(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            APP_ID);

    private SpaceEnroller spaceEnroller;

    @Mock
    private SpaceEnrollerConfig spaceEnrollerConfig;

    @Mock
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    private <T> ArgumentMatcher<List<T>> anyListOfSize(final int expectedSize) {
        return new ArgumentMatcher<List<T>>() {

            @Override
            public boolean matches(Object object) {
                return List.class.isInstance(object) && List.class.cast(object).size() == expectedSize;
            }
        };
    }

    @Before
    public void buildMocks() throws CloudFoundryException {
        //default
        when(spaceEnrollerConfig.getSpaceId()).thenReturn(SPACE_ID);
        when(spaceEnrollerConfig.getId()).thenReturn(SERVICE_ID);
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> ApplicationIdentity.builder()
                                .guid(applicationId)
                                .name(applicationId)
                                .build())
                        .collect(Collectors.toList()));

        when(deployment.getApplicationId()).thenReturn(APP_ID);

        spaceEnroller = spy(SpaceEnroller.builder()
                .clock(clock)
                .period(INTERVAL)
                .spaceEnrollerConfigId(SERVICE_ID)
                .spaceEnrollerConfigRepository(spaceEnrollerConfigRepository)
                .cloudFoundryApi(cloudFoundryApi)
                .applicationRepository(applicationRepository)
                .deployment(deployment)
                .build());
    }

    @Test
    public void testNewAppeared() throws Exception {
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        when(spaceEnrollerConfig.getExcludeFromAutoEnrollment()).thenReturn(null);
        //it will return every ids
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return app id
                .filter(remoteApplicationId -> !remoteApplicationId.equals(APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfoLinkedToService(remoteApplicationId,
                        "another_service_id"))
                .collect(Collectors.toList()));
        spaceEnroller.run();

        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
        verify(cloudFoundryApi, times(1)).listApplications(eq(SPACE_ID), eq(null));
        //Normally remote app has not be bound
        verify(cloudFoundryApi, times(1))
                .bindServiceInstance(argThat(anyListOfSize(remoteApplicationIds.size() - 1)),
                        anyString());

        Pattern pattern = Pattern.compile(".*");

        when(spaceEnrollerConfig.getExcludeFromAutoEnrollment()).thenReturn(pattern);
        spaceEnroller.run();
        verify(cloudFoundryApi, times(1)).listApplications(eq(SPACE_ID), eq(pattern));

    }

    @Test
    public void testNoNew() throws Exception {
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        //it will return every ids except final one
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return app id
                .filter(remoteApplicationId -> !remoteApplicationId.equals(APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfoLinkedToService(remoteApplicationId,
                        SERVICE_ID))
                .collect(Collectors.toList()));
        spaceEnroller.run();

        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
        verify(cloudFoundryApi, never())
                .bindServiceInstance(anyListOf(ApplicationIdentity.class), anyString());

    }

    @Test
    public void testRunRemoteError() throws CloudFoundryException, EntityNotFoundException {
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(spaceEnrollerConfig);
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenThrow(new CloudFoundryException(null))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> ApplicationIdentity.builder()
                                .guid(applicationId)
                                .name(applicationId)
                                .build())
                        .collect(Collectors.toList()));
        doThrow(new CloudFoundryException(
                new EntityNotFoundException(EntityType.service, SERVICE_ID)))
                .when(cloudFoundryApi)
                .bindServiceInstance(anyListOf(ApplicationIdentity.class), eq(SERVICE_ID));
        //First list call fails
        spaceEnroller.run();
        verify(spaceEnroller, times(1)).rescheduleWithDefaultPeriod();
        //Second bind call fails
        spaceEnroller.run();
        verify(spaceEnroller, times(2)).rescheduleWithDefaultPeriod();

    }

    @Test
    public void testRunServiceDeleted() {
        when(spaceEnrollerConfigRepository.findOne(eq(SERVICE_ID))).thenReturn(null);
        spaceEnroller.run();
        verify(clock, times(1)).removeTask(eq(SERVICE_ID));
        verify(spaceEnroller, never()).rescheduleWithDefaultPeriod();
    }

}