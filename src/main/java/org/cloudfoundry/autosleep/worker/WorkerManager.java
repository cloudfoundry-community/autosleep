package org.cloudfoundry.autosleep.worker;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class WorkerManager implements WorkerManagerService {

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
        bindingRepository.findAll().forEach(applicationBinding -> {
            SpaceEnrollerConfig spaceEnrollerConfig =
                    serviceRepository.findOne(applicationBinding.getServiceInstanceId());
            if (spaceEnrollerConfig != null) {
                registerApplicationStopper(spaceEnrollerConfig, applicationBinding.getApplicationId());
            }
        });
        serviceRepository.findAll().forEach(this::registerSpaceEnroller);
    }

    @Override
    public void registerApplicationStopper(SpaceEnrollerConfig config, String applicationId) {
        Duration interval = serviceRepository.findOne(config.getId()).getIdleDuration();
        log.debug("Initializing a watch on app {}, for an idleDuration of {} ", applicationId,
                interval.toString());
        ApplicationStopper checker = ApplicationStopper.builder()
                .clock(clock)
                .period(interval)
                .appUid(UUID.fromString(applicationId))
                .cloudFoundryApi(cloudFoundryApi)
                .spaceEnrollerConfigId(config.getId())
                .taskId(config.getId() + "-" + applicationId)
                .applicationRepository(applicationRepository)
                .applicationLocker(applicationLocker)
                .build();
        checker.startNow();
    }

    @Override
    public void registerSpaceEnroller(SpaceEnrollerConfig service) {
        SpaceEnroller spaceEnroller = SpaceEnroller.builder()
                .clock(clock)
                .period(service.getIdleDuration())
                .spaceEnrollerConfigId(service.getId())
                .cloudFoundryApi(cloudFoundryApi)
                .serviceRepository(serviceRepository)
                .applicationRepository(applicationRepository)
                .deployment(deployment)
                .build();
        spaceEnroller.start(Config.DELAY_BEFORE_FIRST_SERVICE_CHECK);
    }


}
