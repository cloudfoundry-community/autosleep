package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
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

    private BindingRepository bindingRepository;

    private ServiceRepository serviceRepository;

    private ApplicationRepository applicationRepository;

    private CloudFoundryApiService cloudFoundryApi;


    @Autowired
    public GlobalWatcher(Clock clock, BindingRepository bindingRepository,
                         ServiceRepository serviceRepository, ApplicationRepository applicationRepository,
                         CloudFoundryApiService cloudFoundryApi) {
        this.clock = clock;
        this.cloudFoundryApi = cloudFoundryApi;
        this.bindingRepository = bindingRepository;
        this.serviceRepository = serviceRepository;
        this.applicationRepository = applicationRepository;
        this.cloudFoundryApi = cloudFoundryApi;
    }

    @PostConstruct
    public void init() {
        log.debug("Initializer watchers for every app already bound (except if handle by another instance of "
                + "autosleep)");
        bindingRepository.findAll().forEach(this::watchApp);
        serviceRepository.findAll()
                .forEach(autosleepServiceInstance ->
                        watchServiceBindings(autosleepServiceInstance.getServiceInstanceId(), null));
    }

    public void watchApp(ApplicationBinding binding) {
        Duration interval = serviceRepository.findOne(binding.getServiceInstanceId()).getInterval();
        log.debug("Initializing a watch on app {}, for an interval of {} ", binding.getAppGuid(), interval.toString());
        AppStateChecker checker = AppStateChecker.builder()
                .clock(clock)
                .period(interval)
                .appUid(UUID.fromString(binding.getAppGuid()))
                .cloudFoundryApi(cloudFoundryApi)
                .bindingId(binding.getId())
                .applicationRepository(applicationRepository)
                .build();
        checker.startNow();
    }

    public void watchServiceBindings(String serviceId, Duration delayBeforeTreatment) {
        ApplicationBinder applicationBinder = ApplicationBinder.builder()
                .clock(clock)
                .period(Config.defaultServiceBindingRefresh)
                .serviceInstanceId(serviceId)
                .cloudFoundryApi(cloudFoundryApi)
                .serviceRepository(serviceRepository)
                .applicationRepository(applicationRepository)
                .build();
        applicationBinder.start(delayBeforeTreatment);
    }


}
