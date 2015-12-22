package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
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

    private Deployment deployment;

    private ApplicationLocker applicationLocker;


    @Autowired
    public GlobalWatcher(Clock clock, BindingRepository bindingRepository,
                         ServiceRepository serviceRepository, ApplicationRepository applicationRepository,
                         CloudFoundryApiService cloudFoundryApi,
                         Deployment deployment, ApplicationLocker applicationLocker) {
        this.clock = clock;
        this.cloudFoundryApi = cloudFoundryApi;
        this.bindingRepository = bindingRepository;
        this.serviceRepository = serviceRepository;
        this.applicationRepository = applicationRepository;
        this.cloudFoundryApi = cloudFoundryApi;
        this.deployment = deployment;
        this.applicationLocker = applicationLocker;
    }

    @PostConstruct
    public void init() {
        log.debug("Initializer watchers for every app already bound (except if handle by another instance of "
                + "autosleep)");
        bindingRepository.findAll().forEach(this::watchApp);
        serviceRepository.findAll()
                .forEach(autosleepServiceInstance ->
                        watchServiceBindings(autosleepServiceInstance, null));
    }

    public void watchApp(ApplicationBinding binding) {
        Duration interval = serviceRepository.findOne(binding.getServiceInstanceId()).getIdleDuration();
        log.debug("Initializing a watch on app {}, for an idleDuration of {} ", binding.getApplicationId(),
                interval.toString());
        ApplicationStopper checker = ApplicationStopper.builder()
                .clock(clock)
                .period(interval)
                .appUid(UUID.fromString(binding.getApplicationId()))
                .cloudFoundryApi(cloudFoundryApi)
                .serviceInstanceId(binding.getServiceInstanceId())
                .bindingId(binding.getServiceBindingId())
                .applicationRepository(applicationRepository)
                .applicationLocker(applicationLocker)
                .build();
        checker.startNow();
    }

    public void watchServiceBindings(SpaceEnrollerConfig service, Duration delayBeforeTreatment) {
        SpaceEnroller spaceEnroller = SpaceEnroller.builder()
                .clock(clock)
                .period(service.getIdleDuration())
                .serviceInstanceId(service.getServiceInstanceId())
                .cloudFoundryApi(cloudFoundryApi)
                .serviceRepository(serviceRepository)
                .applicationRepository(applicationRepository)
                .deployment(deployment)
                .build();
        spaceEnroller.start(delayBeforeTreatment);
    }


}
