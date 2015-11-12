package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class GlobalWatcher {

    private Clock clock;

    private CloudFoundryApiService remote;

    private BindingRepository bindingRepository;

    private ServiceRepository serviceRepository;

    @Autowired
    public GlobalWatcher(Clock clock, CloudFoundryApiService remote, BindingRepository bindingRepository,
                         ServiceRepository serviceRepository) {
        this.clock = clock;
        this.remote = remote;
        this.bindingRepository = bindingRepository;
        this.serviceRepository = serviceRepository;
    }

    @PostConstruct
    public void init() {
        log.debug("Initializer watchers for every app already bound (except if handle by another instance of "
                + "autosleep)");
        bindingRepository.findAll().forEach(this::watchApp);
    }


    public void watchApp(AutoSleepServiceBinding binding) {
        Duration interval = serviceRepository.findOne(binding.getServiceInstanceId()).getInterval();
        log.debug("Initializing a watch on app {}, for an interval of {} ", binding.getAppGuid(), interval.toString());
        AppStateChecker checker = new AppStateChecker(UUID.fromString(binding.getAppGuid()),
                binding.getId(),
                interval,
                remote,
                clock,
                bindingRepository);
        checker.start();
    }

}
