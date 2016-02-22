/**
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

package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.config.Config.RouteBindingParameters;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.RouteBinding;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.RouteBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceBindingResource;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class AutosleepBindingService implements ServiceInstanceBindingService {

    @Autowired
    private ApplicationRepository appRepository;

    @Autowired
    private ApplicationBindingRepository applicationBindingRepository;

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private RouteBindingRepository routeBindingRepository;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Autowired
    private WorkerManagerService workerManager;

    @Autowired
    private CloudFoundryApiService cfApi;

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(
            CreateServiceInstanceBindingRequest request)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {

        final String bindingId = request.getBindingId();
        final String configId = request.getServiceInstanceId();

        log.debug("createServiceInstanceBinding - {}", bindingId);
        SpaceEnrollerConfig spaceEnrollerConfig = spaceEnrollerConfigRepository.findOne(configId);

        String targetAppId = (String) request.getBindResource().get(
                ServiceBindingResource.BIND_RESOURCE_KEY_APP.toString());
        String routeId = (String) request.getBindResource().get(
                ServiceBindingResource.BIND_RESOURCE_KEY_ROUTE.toString());
        if (targetAppId != null) {
            log.debug("creating binding {} for app {}", bindingId, targetAppId);
            ApplicationBinding binding = ApplicationBinding.builder().serviceInstanceId(configId)
                    .serviceBindingId(bindingId)
                    .applicationId(targetAppId).build();
            applicationLocker.executeThreadSafe(targetAppId, () -> {
                ApplicationInfo appInfo = appRepository.findOne(targetAppId);
                if (appInfo == null) {
                    appInfo = new ApplicationInfo(targetAppId);
                }

                appInfo.getEnrollmentState().addEnrollmentState(configId);

                //retrieve service to return its params as credentials

                applicationBindingRepository.save(binding);
                appRepository.save(appInfo);
                workerManager.registerApplicationStopper(spaceEnrollerConfig, targetAppId, bindingId);
            });
            return new CreateServiceInstanceBindingResponse(
                    Collections.singletonMap(
                            Config.ServiceInstanceParameters.IDLE_DURATION,
                            spaceEnrollerConfig.getIdleDuration().toString()));
        } else if (routeId != null) {
            log.debug("creating binding {} for route {}", bindingId, routeId);
            String proxyRoute = "";
            String linkedAppId = (String) request.getParameters().get(RouteBindingParameters.linkedApplicationId);
            String linkedAppBindingId = (String) request.getParameters().get(RouteBindingParameters
                    .linkedApplicationBindingId);
            ApplicationInfo applicationInfo = appRepository.findOne(linkedAppId);
            if (linkedAppId == null || linkedAppBindingId == null || applicationInfo == null) {
                throw new ServiceBrokerException("Only Autosleep is allowed to bind route to itself");
            }
            //TODO ROUTE SERVICE create proxy route?
            String localProxyRoute = Path.PROXY_CONTEXT + "/??";
            routeBindingRepository.save(RouteBinding.builder()
                    .bindingId(bindingId)
                    .routeId(routeId)
                    .configurationId(configId)
                    .localRoute(localProxyRoute)
                    .linkedApplicationId(linkedAppId)
                    .linkedApplicationBindingId(linkedAppBindingId)
                    .build());

            return new CreateServiceInstanceBindingResponse(
                    Collections.singletonMap(
                            Config.ServiceInstanceParameters.IDLE_DURATION,
                            spaceEnrollerConfig.getIdleDuration().toString()),
                    proxyRoute);
        } else {
            throw new ServiceBrokerException("Unknown bind resource");
        }

    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
            throws ServiceBrokerException {
        final String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);

        final ApplicationBinding appBinding = applicationBindingRepository.findOne(bindingId);
        if (appBinding != null) {
            final String appId = appBinding.getApplicationId();

            SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository.findOne(request.getServiceInstanceId());

            //TODO check if need to add in lock
            //TODO add "findByLinkedApp" in repo?
            Iterable<RouteBinding> allBindings = routeBindingRepository.findAll();
            if (allBindings != null) {
                Optional<RouteBinding> linkedRouteBinding = StreamSupport
                        .stream(routeBindingRepository.findAll().spliterator(), true)
                        .filter(routeBinding -> routeBinding.getLinkedApplicationId().equals(appId))
                        .findFirst();
                if (linkedRouteBinding.isPresent()
                        && linkedRouteBinding.get().getLinkedApplicationBindingId().equals(bindingId)) {
                    log.debug("detected associated route binding {}, cleaning it",
                            linkedRouteBinding.get().getBindingId());
                    try {
                        //we had a proxy route binding for this app, clean it before remove app binding
                        cfApi.unbind(linkedRouteBinding.get().getBindingId());
                    } catch (CloudFoundryException e) {
                        throw new ServiceBrokerException("Autosleep was unable to clear related route binding.");
                    }
                }
            }

            applicationLocker.executeThreadSafe(appId, () -> {
                log.debug("deleteServiceInstanceBinding on app ", appId);
                ApplicationInfo appInfo = appRepository.findOne(appId);
                if (appInfo != null) {
                    appInfo.getEnrollmentState()
                            .updateEnrollment(serviceInstance.getId(), !serviceInstance.isForcedAutoEnrollment());
                    if (appInfo.getEnrollmentState().getStates().isEmpty()) {
                        appRepository.delete(appId);
                        applicationLocker.removeApplication(appId);
                    } else {
                        appRepository.save(appInfo);
                    }
                } else {
                    log.error("Deleting a binding with no related application info. This should never happen.");
                }
                applicationBindingRepository.delete(bindingId);

                //task launched will cancel by itself
            });
        } else {
            final RouteBinding routeBinding = routeBindingRepository.findOne(bindingId);
            //TODO ROUTE SERVICE : delete local proxy route
            routeBindingRepository.delete(routeBinding.getBindingId());
        }

    }
}
