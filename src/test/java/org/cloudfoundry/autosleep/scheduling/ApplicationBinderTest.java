package org.cloudfoundry.autosleep.scheduling;

import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException;
import org.cloudfoundry.autosleep.remote.EntityNotFoundException.EntityType;
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
public class ApplicationBinderTest {
    private static final Duration INTERVAL = Duration.ofMillis(300);

    private static final String SERVICE_ID = "serviceId";

    private static final UUID SPACE_ID = UUID.randomUUID();

    private static final UUID APP_ID = UUID.randomUUID();


    @Mock
    private Clock clock;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AutosleepServiceInstance autosleepServiceInstance;

    @Mock
    private Deployment deployment;


    private ApplicationBinder applicationBinder;

    private List<UUID> remoteApplicationIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            APP_ID);


    @Before
    public void buildMocks() throws CloudFoundryException {
        //default
        when(autosleepServiceInstance.getSpaceId()).thenReturn(SPACE_ID.toString());
        when(autosleepServiceInstance.getServiceInstanceId()).thenReturn(SERVICE_ID);
        when(serviceRepository.findOne(eq(SERVICE_ID))).thenReturn(autosleepServiceInstance);
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> new ApplicationIdentity(applicationId, applicationId.toString()))
                        .collect(Collectors.toList()));

        when(deployment.getApplicationId()).thenReturn(APP_ID);

        applicationBinder = spy(ApplicationBinder.builder()
                .clock(clock)
                .period(INTERVAL)
                .serviceInstanceId(SERVICE_ID)
                .cloudFoundryApi(cloudFoundryApi)
                .serviceRepository(serviceRepository)
                .applicationRepository(applicationRepository)
                .deployment(deployment)
                .build());
    }


    @Test
    public void testNewAppeared() throws Exception {
        when(serviceRepository.findOne(eq(SERVICE_ID))).thenReturn(autosleepServiceInstance);
        when(autosleepServiceInstance.getExcludeFromAutoEnrollment()).thenReturn(null);
        //it will return every ids
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return app id
                .filter(remoteApplicationId -> !remoteApplicationIds.equals(APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfo(remoteApplicationId, "another_service_id"))
                .collect(Collectors.toList()));
        applicationBinder.run();

        verify(applicationBinder, times(1)).rescheduleWithDefaultPeriod();
        verify(cloudFoundryApi, times(1)).listApplications(eq(SPACE_ID), eq(null));
        //Normaly remote app has not be bound
        verify(cloudFoundryApi, times(1))
                .bindServiceInstance(argThat(anyListOfSize(remoteApplicationIds.size() - 1, ApplicationIdentity.class)),
                        anyString());

        Pattern pattern = Pattern.compile(".*");

        when(autosleepServiceInstance.getExcludeFromAutoEnrollment()).thenReturn(pattern);
        applicationBinder.run();
        verify(cloudFoundryApi, times(1)).listApplications(eq(SPACE_ID), eq(pattern));

    }

    @Test
    public void testNoNew() throws Exception {
        when(serviceRepository.findOne(eq(SERVICE_ID))).thenReturn(autosleepServiceInstance);
        //it will return every ids except final one
        when(applicationRepository.findAll()).thenReturn(remoteApplicationIds.stream()
                //do not return app id
                .filter(remoteApplicationId -> !remoteApplicationIds.equals(APP_ID))
                .map(remoteApplicationId -> BeanGenerator.createAppInfo(remoteApplicationId, SERVICE_ID))
                .collect(Collectors.toList()));
        applicationBinder.run();

        verify(applicationBinder, times(1)).rescheduleWithDefaultPeriod();
        verify(cloudFoundryApi, never())
                .bindServiceInstance(anyListOf(ApplicationIdentity.class), anyString());

    }


    @Test
    public void testRunServiceDeleted() {
        when(serviceRepository.findOne(eq(SERVICE_ID))).thenReturn(null);
        applicationBinder.run();
        verify(clock, times(1)).removeTask(eq(SERVICE_ID));
        verify(applicationBinder, never()).rescheduleWithDefaultPeriod();
    }

    @Test
    public void testRunRemoteError() throws CloudFoundryException,  EntityNotFoundException {
        when(serviceRepository.findOne(eq(SERVICE_ID))).thenReturn(autosleepServiceInstance);
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(cloudFoundryApi.listApplications(eq(SPACE_ID), any(Pattern.class)))
                .thenThrow(new CloudFoundryException(null))
                .thenReturn(remoteApplicationIds.stream()
                        .map(applicationId -> new ApplicationIdentity(applicationId, applicationId.toString()))
                        .collect(Collectors.toList()));
        doThrow(new EntityNotFoundException(EntityType.service, SERVICE_ID)).when(cloudFoundryApi)
                .bindServiceInstance(anyListOf(ApplicationIdentity.class), eq(SERVICE_ID));
        //First list call fails
        applicationBinder.run();
        verify(applicationBinder, times(1)).rescheduleWithDefaultPeriod();
        //Second bind call fails
        applicationBinder.run();
        verify(applicationBinder, times(2)).rescheduleWithDefaultPeriod();


    }


    private <T> ArgumentMatcher<List<T>> anyListOfSize(final int expectedSize, Class<T> objectClass) {
        return new ArgumentMatcher<List<T>>() {
            @Override
            public boolean matches(Object object) {
                return List.class.isInstance(object) && List.class.cast(object).size() == expectedSize;
            }
        };
    }

  /*  @Test
    public void testNewAppearedAlreadyLinkedToAnotherOne() throws Exception {
        when(serviceRepository.findOne(eq(SERVICE_ID))).thenReturn(autosleepServiceInstance);


        when(autosleepServiceInstance.getExcludeFromAutoEnrollment()).thenReturn(null);

        //it will return every ids
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());

        applicationBinder.run();

        verify(applicationBinder, times(1)).rescheduleWithDefaultPeriod();
        verify(cloudFoundryApi, times(1)).listApplications(eq(SPACE_ID), eq(null));

        //Normaly remote app has not be binded
        verify(cloudFoundryApi, times(1))
                .bindServiceInstance(argThat(anyListOfSize(remoteApplicationIds.size() - 1, ApplicationIdentity.class)),
                        anyString());

        Pattern pattern = Pattern.compile(".*");
        when(autosleepServiceInstance.getExcludeFromAutoEnrollment()).thenReturn(pattern);

        remoteApplicationIds.add(UUID.randomUUID());
        applicationBinder.run();
        verify(cloudFoundryApi, times(1)).listApplications(eq(SPACE_ID), eq(pattern));
        verify(cloudFoundryApi, times(1))
                .bindServiceInstance(argThat(anyListOfSize(1, ApplicationIdentity.class)),
                        anyString());


    }*/


}