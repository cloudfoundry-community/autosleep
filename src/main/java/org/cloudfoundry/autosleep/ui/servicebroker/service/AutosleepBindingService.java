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
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.Binding;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.cloudfoundry.autosleep.dao.model.Binding.ResourceType.Application;
import static org.cloudfoundry.autosleep.dao.model.Binding.ResourceType.Route;

@Service
@Slf4j
public class AutosleepBindingService implements ServiceInstanceBindingService {

    @Autowired
    private ApplicationRepository appRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Autowired
    private WorkerManagerService workerManager;

    @Autowired
    private CloudFoundryApiService cfApi;

    @Autowired
    private DeployedApplicationConfig.Deployment deployment;

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

        Binding.BindingBuilder bindingBuilder = Binding.builder().serviceInstanceId(configId)
                .serviceBindingId(bindingId);

        if (targetAppId != null) {
            log.debug("creating binding {} for app {}", bindingId, targetAppId);
            bindingBuilder.resourceId(targetAppId)
                    .resourceType(Application);
            applicationLocker.executeThreadSafe(targetAppId, () -> {
                ApplicationInfo appInfo = appRepository.findOne(targetAppId);
                if (appInfo == null) {
                    appInfo = new ApplicationInfo(targetAppId);
                }

                appInfo.getEnrollmentState().addEnrollmentState(configId);

                //retrieve service to return its params as credentials
                bindingRepository.save(bindingBuilder.build());
                appRepository.save(appInfo);
                workerManager.registerApplicationStopper(spaceEnrollerConfig, targetAppId, bindingId);
            });
            return new CreateServiceInstanceBindingResponse(
                    Collections.singletonMap(
                            Config.ServiceInstanceParameters.IDLE_DURATION,
                            spaceEnrollerConfig.getIdleDuration().toString()));
        } else if (routeId != null) {
            log.debug("creating binding {} for route {}", bindingId, routeId);

            //TODO check how to check that only autosleep can bind a route to itself.
            //check we know at least one app or all?
            List<ApplicationResource> apps = null;
            try {
                apps = cfApi.listRouteApplications(routeId);
                if (!apps.stream().allMatch(applicationResource -> appRepository.findOne(applicationResource
                        .getMetadata().getId()) != null)) {
                    throw new ServiceBrokerException("Only Autosleep is allowed to bind route to itself");
                }
            } catch (CloudFoundryException e) {
                throw new ServiceBrokerException("Can't check applications map to this route id");
            }

            bindingBuilder.resourceId(routeId)
                    .resourceType(Route);

            bindingRepository.save(bindingBuilder.build());

            String firstUri = deployment.getFirstUri();
            if (firstUri == null) {
                firstUri = "local-deployment";
            }

            return new CreateServiceInstanceBindingResponse(
                    Collections.singletonMap(
                            Config.ServiceInstanceParameters.IDLE_DURATION,
                            spaceEnrollerConfig.getIdleDuration().toString()),
                    firstUri + Config.Path.PROXY_CONTEXT + "/" + bindingId);
        } else {
            throw new ServiceBrokerException("Unknown bind resource");
        }

    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
            throws ServiceBrokerException {
        final String bindingId = request.getBindingId();
        log.debug("deleteServiceInstanceBinding - {}", bindingId);

        final Binding binding = bindingRepository.findOne(bindingId);
        if (binding.getResourceType() == Application) {
            final String appId = binding.getResourceId();

            SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository.findOne(request.getServiceInstanceId());

            //TODO check if need to add in lock

            try {
                //call CFAPI to get routes associated to app
                List<String> mappedRouteIds = cfApi.listApplicationRoutes(appId)
                        .stream().map(
                                routeResource -> routeResource.getMetadata().getId()
                        ).collect(Collectors.toList());

                //retrieve saved route bindings and compare
                if (bindingRepository.count() > 0) {
                    List<Binding> linkedRouteBindings = StreamSupport
                            .stream(bindingRepository.findAll().spliterator(), true)
                            .filter(routeBinding -> routeBinding.getResourceType() == Route)
                            .filter(routeBinding -> mappedRouteIds.contains(routeBinding.getResourceId()))
                            .collect(Collectors.toList());

                    //clean all bindings in common set, provided they are related to the same service instance
                    linkedRouteBindings.stream()
                            .filter(linkedRouteBinding -> linkedRouteBinding.getServiceInstanceId().equals(
                                    serviceInstance.getId()))
                            .forEach(linkedRouteBinding -> {
                                log.debug("detected associated route binding {}, cleaning it", linkedRouteBinding
                                        .getServiceBindingId());
                                try {
                                    //we had a proxy route binding for this app, clean it before remove app binding
                                    cfApi.unbind(linkedRouteBinding.getServiceBindingId());
                                } catch (CloudFoundryException e) {
                                    throw new ServiceBrokerException("Autosleep was unable to clear related route "
                                            + "binding.");
                                }
                            });
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
                    bindingRepository.delete(bindingId);

                    //task launched will cancel by itself
                });
            } catch (CloudFoundryException e) {
                throw new ServiceBrokerException("Couldn't clean related app bindings", e);
            }

        } else {
            bindingRepository.delete(bindingId);
        }

    }
}
