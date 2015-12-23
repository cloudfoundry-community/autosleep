package org.cloudfoundry.autosleep.business;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.business.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.business.scheduling.Clock;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class GlobalWatcher {

    @Autowired
    private Clock clock;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CloudFoundryApiService cloudFoundryApi;

    @Autowired
    private DeployedApplicationConfig.Deployment deployment;

    @Autowired
    private ApplicationLocker applicationLocker;


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
