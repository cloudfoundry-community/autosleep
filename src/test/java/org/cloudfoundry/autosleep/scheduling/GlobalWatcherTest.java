package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
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
    private static final UUID WATCHER_UID = UUID.fromString("F4BB2108-9C21-43C5-98AC-5059F166B23C");
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


    private GlobalWatcher spyWatcher;

    private enum UnattachedBinding {
        unattached01, unattached02,
    }



    @Before
    public void populateDb() {

        //init mock binding repository with unattached binding
        List<AutoSleepServiceBinding> storedBindings = new ArrayList<>();
        Arrays.asList(UnattachedBinding.values()).forEach(id -> {
            AutoSleepServiceBinding binding = new AutoSleepServiceBinding(id.name(),
                    SERVICE_ID, null, null, APP_UID.toString());
            storedBindings.add(binding);
        });

        when(mockBindingRepo.findAll()).thenReturn(storedBindings);


        //init mock serviceRepo
        AutoSleepServiceInstance mockService = mock(AutoSleepServiceInstance.class);
        when(mockService.getInterval()).thenReturn(INTERVAL);
        when(mockServiceRepo.findOne(any())).thenReturn(mockService);

        spyWatcher = spy(new GlobalWatcher(clock, mockRemote, mockBindingRepo, mockServiceRepo));
    }

    @Test
    public void testInit() {
        spyWatcher.init();
        verify(spyWatcher, times(UnattachedBinding.values().length)).watchApp(any());
    }

    @Test
    public void testWatchApp() {
        AutoSleepServiceBinding binding = new AutoSleepServiceBinding("testWatch",
                SERVICE_ID, null, null, APP_UID.toString());
        spyWatcher.watchApp(binding);
        verify(mockBindingRepo, times(1)).save(any(AutoSleepServiceBinding.class));
    }

    @Test
    public void testCancelWatch()  {
        AutoSleepServiceBinding binding = new AutoSleepServiceBinding("testWatch", SERVICE_ID, null, null,
                APP_UID.toString());
        spyWatcher.cancelWatch(binding);
        verify(mockBindingRepo, times(1)).save(any(AutoSleepServiceBinding.class));

        reset(mockBindingRepo);
        spyWatcher.cancelWatch(null);
        verify(mockBindingRepo, never()).save(any(AutoSleepServiceBinding.class));
    }

    @Test
    public void testOnStop()  {
        spyWatcher.init();
        spyWatcher.cleanup();
        verify(spyWatcher, times(UnattachedBinding.values().length)).cancelWatch(any());
    }
}