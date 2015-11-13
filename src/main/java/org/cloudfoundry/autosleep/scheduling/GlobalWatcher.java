package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class GlobalWatcher {

    private Clock clock;

    private CloudFoundryApiService remote;

    private BindingRepository bindingRepository;

    private ServiceRepository serviceRepository;

    private ApplicationRepository applicationRepository;

    @Autowired
    public GlobalWatcher(Clock clock, CloudFoundryApiService remote, BindingRepository bindingRepository,
                         ServiceRepository serviceRepository, ApplicationRepository applicationRepository) {
        this.clock = clock;
        this.remote = remote;
        this.bindingRepository = bindingRepository;
        this.serviceRepository = serviceRepository;
        this.applicationRepository = applicationRepository;
    }

    @PostConstruct
    public void init() {
        log.debug("Initializer watchers for every app already bound (except if handle by another instance of "
                + "autosleep)");
        bindingRepository.findAll().forEach(this::watchApp);
    }

    public void watchApp(ApplicationBinding binding) {
        Duration interval = serviceRepository.findOne(binding.getServiceInstanceId()).getInterval();
        log.debug("Initializing a watch on app {}, for an interval of {} ", binding.getAppGuid(), interval.toString());
        AppStateChecker checker = new AppStateChecker(UUID.fromString(binding.getAppGuid()),
                binding.getId(),
                interval,
                remote,
                clock,
                applicationRepository);
        checker.start();
    }


}
