/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.worker;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;

import static org.cloudfoundry.autosleep.dao.model.Binding.ResourceType.Application;

@Slf4j
@Service
public class WorkerManager implements WorkerManagerService {

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private BindingRepository bindingRepository;

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
        bindingRepository.findAllByResourceType(Application).forEach(applicationBinding -> {
            SpaceEnrollerConfig spaceEnrollerConfig =
                    spaceEnrollerConfigRepository.findOne(applicationBinding.getServiceInstanceId());
            if (spaceEnrollerConfig != null) {
                registerApplicationStopper(spaceEnrollerConfig,
                        applicationBinding.getResourceId(),
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
