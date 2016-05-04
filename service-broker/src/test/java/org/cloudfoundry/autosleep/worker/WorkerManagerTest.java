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
import org.cloudfoundry.autosleep.worker.ApplicationStopper;
import org.cloudfoundry.autosleep.worker.SpaceEnroller;
import org.cloudfoundry.autosleep.worker.WorkerManager;
import org.cloudfoundry.autosleep.access.dao.model.Binding;
import org.cloudfoundry.autosleep.access.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.access.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationIdentity;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.cloudfoundry.autosleep.access.dao.model.Binding.ResourceType.Application;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class WorkerManagerTest {

    private static final String APPLICATION_ID = UUID.randomUUID().toString();

    private static final Duration INTERVAL = Duration.ofMillis(300);

    @Mock
    private ApplicationLocker applicationLocker;

    @Mock
    private Clock clock;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private DeployedApplicationConfig.Deployment deployment;

    @Mock
    private ApplicationRepository mockAppRepo;

    @Mock
    private BindingRepository mockBindingRepo;

    @Mock
    private CloudFoundryApiService mockRemote;

    @Mock
    private SpaceEnrollerConfigRepository mockServiceRepo;

    private List<UUID> remoteApplications = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());

    private List<String> serviceIds = Arrays.asList("serviceId1", "serviceId2");

    @InjectMocks
    @Spy
    private WorkerManager spyWatcher;

    private List<String> unattachedBinding = Arrays.asList("unattached01", "unattached02");

    @Before
    public void populateDb() throws CloudFoundryException {
        doAnswer(invocationOnMock -> {
            ((Runnable) invocationOnMock.getArguments()[1]).run();
            return null;
        }).when(applicationLocker).executeThreadSafe(anyString(), any(Runnable.class));

        //init mock binding repository with unattached binding
        List<Binding> storedBindings = unattachedBinding.stream()
                .map(id -> BeanGenerator.createBinding())
                .collect(Collectors.toList());

        when(mockBindingRepo.findAllByResourceType(Application)).thenReturn(storedBindings);

        //init mock serviceRepo
        SpaceEnrollerConfig mockService = mock(SpaceEnrollerConfig.class);
        when(mockService.getIdleDuration()).thenReturn(INTERVAL);
        when(mockServiceRepo.findOne(any())).thenReturn(mockService);

        List<SpaceEnrollerConfig> fakeServices = serviceIds.stream()
                .map(BeanGenerator::createServiceInstance)
                .collect(Collectors.toList());
        when(mockServiceRepo.findAll()).thenReturn(fakeServices);

        when(cloudFoundryApi.listApplications(any(String.class), any(Pattern.class)))
                .thenReturn(remoteApplications.stream()
                        .map(id -> ApplicationIdentity.builder()
                                .guid(id.toString())
                                .name(id.toString())
                                .build())
                        .collect(Collectors.toList()));

    }

    @Test
    public void testInit() {
        spyWatcher.init();
        verify(spyWatcher, times(unattachedBinding.size()))
                .registerApplicationStopper(any(SpaceEnrollerConfig.class), anyString(), anyString());
        verify(spyWatcher, times(serviceIds.size())).registerSpaceEnroller(any(SpaceEnrollerConfig.class));
    }

    @Test
    public void test_enrollment_task_is_scheduled() throws Exception {
        String serviceId = "serviceId";
        spyWatcher.registerSpaceEnroller(BeanGenerator.createServiceInstance(serviceId));
        verify(clock).scheduleTask(eq(serviceId), eq(Config.DELAY_BEFORE_FIRST_SERVICE_CHECK),
                any(SpaceEnroller.class));
    }

    @Test
    public void test_task_of_stop_is_scheduled() {
        SpaceEnrollerConfig config = BeanGenerator.createServiceInstance();
        spyWatcher.registerApplicationStopper(config, APPLICATION_ID, "bindingid");
        verify(clock).scheduleTask(anyString(), eq(Duration.ofSeconds(0)),
                any(ApplicationStopper.class));
    }

}