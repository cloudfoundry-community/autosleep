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

package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.access.dao.model.Binding;
import org.cloudfoundry.autosleep.access.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.access.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.access.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.ServiceInstanceParameters.Enrollment;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.cloudfoundry.autosleep.worker.WorkerManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.ServiceBindingResource;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static org.cloudfoundry.autosleep.access.dao.model.Binding.ResourceType.Application;
import static org.cloudfoundry.autosleep.access.dao.model.Binding.ResourceType.Route;

@Service
@Slf4j
public class AutosleepBindingService implements ServiceInstanceBindingService {

    @Autowired
    private ApplicationRepository appRepository;

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private CloudFoundryApiService cfApi;

    @Autowired
    private DeployedApplicationConfig.Deployment deployment;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Autowired
    private WorkerManagerService workerManager;

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(
            CreateServiceInstanceBindingRequest request)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {

        final String bindingId = request.getBindingId();
        final String configId = request.getServiceInstanceId();

        log.debug("createServiceInstanceBinding - {}", bindingId);
        SpaceEnrollerConfig spaceEnrollerConfig = spaceEnrollerConfigRepository.findOne(configId);

        String targetAppId = (String) request.getBindResource()
                .get(ServiceBindingResource.BIND_RESOURCE_KEY_APP.toString());
        String route = (String) request.getBindResource()
                .get(ServiceBindingResource.BIND_RESOURCE_KEY_ROUTE.toString());

        Binding.BindingBuilder bindingBuilder = Binding.builder()
                .serviceInstanceId(configId)
                .serviceBindingId(bindingId);

        if (targetAppId != null) {
            log.info("Creating binding {} for app {}", bindingId, targetAppId);
            bindingBuilder.resourceId(targetAppId)
                    .resourceType(Application);
            applicationLocker.executeThreadSafe(targetAppId, () -> {
                ApplicationInfo appInfo = appRepository.findOne(targetAppId);
                if (appInfo == null) {
                    appInfo = ApplicationInfo.builder()
                            .uuid(targetAppId)
                            .build();
                }

                appInfo.getEnrollmentState().addEnrollmentState(configId);

                //retrieve service to return its params as credentials
                bindingRepository.save(bindingBuilder.build());
                appRepository.save(appInfo);
                workerManager.registerApplicationStopper(spaceEnrollerConfig, targetAppId, bindingId);
            });
            return new CreateServiceInstanceAppBindingResponse().withCredentials(Collections.singletonMap(
                    Config.ServiceInstanceParameters.IDLE_DURATION, spaceEnrollerConfig.getIdleDuration().toString()));
        } else if (route != null) {

            //TODO once route services handle stopped apps
            // retrieved targetAppId in arbitraryParams
            // retrieve proxy url (in settings)
            // send redirection route
            // String redirectionRoute = "https://" + proxyURL + Config.Path.PROXY_CONTEXT + "/fix/" + targetAppId;
            log.error("AutoWakeup based on route services not implemented yet. ");
            return null;
        } else {
            throw new ServiceBrokerException("Unknown bind resource type");
        }
    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
            throws ServiceBrokerException {
        final String bindingId = request.getBindingId();
        final String serviceId = request.getServiceInstanceId();
        log.debug("deleteServiceInstanceBinding - {} on service {}", bindingId, serviceId);

        final Binding binding = bindingRepository.findOne(bindingId);
        if (binding == null) {
            log.error("Trying to delete unknown binding {}, letting it pass", bindingId);
            return;
        }
        if (binding.getResourceType() == Application) {
            log.info("Unbinding app {} (binding {})", binding.getResourceId(), bindingId);
            final String appId = binding.getResourceId();

            SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository.findOne(request.getServiceInstanceId());
            log.debug("serviceInstance {}", serviceInstance);
            //TODO check if need to add in lock
            try {
                //call CFAPI to get routes associated to app
                List<String> mappedRouteIds = cfApi.listApplicationRoutes(appId);
                //retrieve saved route bindings and compare, but only if there are still any... 
                //bound routes are still not in place
                if (mappedRouteIds.size() > 0) {
                    List<Binding> linkedRouteBindings = bindingRepository.findByResourceIdAndType(mappedRouteIds, Route);
                    if (linkedRouteBindings.size() > 0) {
                        //clean all bindings in common set, provided they are related to the same service instance
                        linkedRouteBindings.stream()
                                .filter(linkedRouteBinding -> linkedRouteBinding.getServiceInstanceId().equals(serviceId))
                                .forEach(linkedRouteBinding -> {
                                    log.debug("detected associated route binding {}, cleaning it", linkedRouteBinding
                                            .getServiceBindingId());
                                    try {
                                        //we had a proxy route binding for this app, clean it before remove app binding
                                        cfApi.unbind(linkedRouteBinding.getServiceBindingId());
                                    } catch (CloudFoundryException e) {
                                        log.error("Autosleep was unable to clear related route binding {}.",
                                                linkedRouteBinding.getServiceBindingId());
                                        bindingRepository.delete(linkedRouteBinding.getServiceBindingId());
                                    }
                                });
                    }
                }
                applicationLocker.executeThreadSafe(appId,
                        () -> {
                            log.debug("deleteServiceInstanceBinding on app {}", appId);
                            ApplicationInfo appInfo = appRepository.findOne(appId);
                            if (appInfo != null) {
                                appInfo.getEnrollmentState().updateEnrollment(serviceInstance.getId(),
                                        serviceInstance.getEnrollment() != Enrollment.forced
                                                && serviceInstance.getEnrollment() != Enrollment.transient_opt_out);
                                if (appInfo.getEnrollmentState().getStates().isEmpty()) {
                                    appRepository.delete(appId);
                                    applicationLocker.removeApplication(appId);
                                } else {
                                    appRepository.save(appInfo);
                                }
                            } else {
                                log.error("Deleting a binding with no related application info. "
                                        + "This should never happen.");
                            }
                            bindingRepository.delete(bindingId);

                            //task launched will cancel by itself
                        });
            } catch (CloudFoundryException e) {
                throw new ServiceBrokerException("Couldn't clean related app bindings", e);
            }

        } else if (binding.getResourceType() == Route) {
            log.info("Unbinding route {} (binding {})", binding.getResourceId(), bindingId);
            bindingRepository.delete(bindingId);
        }
    }

}
