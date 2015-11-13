package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class GlobalWatcherTest {

    private static final UUID APP_UID = UUID.fromString("9AF63B10-9D25-4162-9AD2-5AA8173FFC3B");
    private static final String SERVICE_ID = "38YF";
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

    private GlobalWatcher spyWatcher;

    private enum UnattachedBinding {
        unattached01, unattached02,
    }

    @Before
    public void populateDb() {

        //init mock binding repository with unattached binding
        List<ApplicationBinding> storedBindings = new ArrayList<>();
        Arrays.asList(UnattachedBinding.values()).forEach(id -> {
            ApplicationBinding binding = new ApplicationBinding(id.name(),
                    SERVICE_ID, null, null, APP_UID.toString());
            storedBindings.add(binding);
        });

        when(mockBindingRepo.findAll()).thenReturn(storedBindings);

        //init mock serviceRepo
        AutosleepServiceInstance mockService = mock(AutosleepServiceInstance.class);
        when(mockService.getInterval()).thenReturn(INTERVAL);
        when(mockServiceRepo.findOne(any())).thenReturn(mockService);

        spyWatcher = spy(new GlobalWatcher(clock, mockRemote, mockBindingRepo, mockServiceRepo, mockAppRepo));
    }

    @Test
    public void testInit() {
        spyWatcher.init();
        verify(spyWatcher, times(UnattachedBinding.values().length)).watchApp(any());
    }

    @Test
    public void testWatchApp() {
        ApplicationBinding binding = new ApplicationBinding("testWatch", SERVICE_ID, null, null, APP_UID.toString());
        spyWatcher.watchApp(binding);
        verify(clock).scheduleTask(eq("testWatch"), eq(Duration.ofSeconds(0)), any(AppStateChecker.class));
    }

}