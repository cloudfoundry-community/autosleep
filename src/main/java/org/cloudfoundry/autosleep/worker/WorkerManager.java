package org.cloudfoundry.autosleep.worker;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Service
public class WorkerManager implements WorkerManagerService {

    @Autowired
    private ApplicationBindingRepository applicationBindingRepository;

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private CloudFoundryApiService cloudFoundryApi;

    @Autowired
    private DeployedApplicationConfig.Deployment deployment;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @PostConstruct
    public void init() {
        log.debug("Initializer watchers for every app already enrolled (except if handle by another instance of "
                + "autosleep)");
        applicationBindingRepository.findAll().forEach(applicationBinding -> {
            SpaceEnrollerConfig spaceEnrollerConfig =
                    spaceEnrollerConfigRepository.findOne(applicationBinding.getServiceInstanceId());
            if (spaceEnrollerConfig != null) {
                registerApplicationStopper(spaceEnrollerConfig, applicationBinding.getApplicationId(),
                        applicationBinding.getServiceBindingId());
            }
        });
        spaceEnrollerConfigRepository.findAll().forEach(this::registerSpaceEnroller);
    }

    @Override
    public void registerApplicationStopper(SpaceEnrollerConfig config, String applicationId, String appBindingId) {
        Duration interval = spaceEnrollerConfigRepository.findOne(config.getId()).getIdleDuration();
        log.debug("Initializing a watch on app {}, for an idleDuration of {} ", applicationId,
                interval.toString());
        ApplicationStopper checker = ApplicationStopper.builder()
                .clock(clock)
                .period(interval)
                .appUid(applicationId)
                .cloudFoundryApi(cloudFoundryApi)
                .spaceEnrollerConfigId(config.getId())
                .bindingId(appBindingId)
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
                .spaceEnrollerConfigRepository(spaceEnrollerConfigRepository)
                .cloudFoundryApi(cloudFoundryApi)
                .applicationRepository(applicationRepository)
                .deployment(deployment)
                .build();
        spaceEnroller.start(Config.DELAY_BEFORE_FIRST_SERVICE_CHECK);
    }

}
