package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class GlobalWatcherTest {

    private static final Duration INTERVAL = Duration.ofMillis(300);

    @Mock
    private Clock clock;

    @Mock
    private CloudFoundryApiService mockRemote;

    @Mock
    private ServiceRepository mockServiceRepo;

    @Mock
    private BindingRepository mockBindingRepo;

    @Mock
    private ApplicationRepository mockAppRepo;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private Deployment deployment;

    @Mock
    private ApplicationLocker applicationLocker;

    @InjectMocks
    @Spy
    private GlobalWatcher spyWatcher;


    private List<String> unattachedBinding = Arrays.asList("unattached01", "unattached02");

    private List<String> serviceIds = Arrays.asList("serviceId1", "serviceId2");

    private List<UUID> remoteApplications = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());


    @Before
    public void populateDb() throws CloudFoundryException {
        doAnswer(invocationOnMock -> {
            ((Runnable)invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));

        //init mock binding repository with unattached binding
        List<ApplicationBinding> storedBindings = unattachedBinding.stream()
                .map(id -> BeanGenerator.createBinding())
                .collect(Collectors.toList());

        when(mockBindingRepo.findAll()).thenReturn(storedBindings);

        //init mock serviceRepo
        AutosleepServiceInstance mockService = mock(AutosleepServiceInstance.class);
        when(mockService.getIdleDuration()).thenReturn(INTERVAL);
        when(mockServiceRepo.findOne(any())).thenReturn(mockService);

        List<AutosleepServiceInstance> fakeServices = serviceIds.stream()
                .map(BeanGenerator::createServiceInstance)
                .collect(Collectors.toList());
        when(mockServiceRepo.findAll()).thenReturn(fakeServices);

        when(cloudFoundryApi.listApplications(any(UUID.class), any(Pattern.class)))
                .thenReturn(remoteApplications.stream()
                        .map(id -> new ApplicationIdentity(id, id.toString())).collect(Collectors.toList()));

    }

    @Test
    public void testInit() {
        spyWatcher.init();
        verify(spyWatcher, times(unattachedBinding.size())).watchApp(any());
        verify(spyWatcher, times(serviceIds.size())).watchServiceBindings(any(), eq(null));
    }

    @Test
    public void testWatchApp() {
        String bindingId = "testWatch";
        ApplicationBinding binding = BeanGenerator.createBinding(bindingId);
        spyWatcher.watchApp(binding);
        verify(clock).scheduleTask(eq(bindingId), eq(Duration.ofSeconds(0)), any(AppStateChecker.class));
    }


    @Test
    public void testWatchServiceBindings() throws Exception {
        String serviceId = "serviceId";

        spyWatcher.watchServiceBindings(BeanGenerator.createServiceInstance(serviceId), Duration.ofSeconds(5));
        verify(clock).scheduleTask(eq(serviceId), eq(Duration.ofSeconds(5)), any(ApplicationBinder.class));

        spyWatcher.watchServiceBindings(BeanGenerator.createServiceInstance(serviceId), null);
        verify(clock).scheduleTask(eq(serviceId), eq(Duration.ofSeconds(0)), any(ApplicationBinder.class));
    }


}