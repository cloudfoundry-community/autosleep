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

package org.cloudfoundry.autosleep.ui.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.Binding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.ui.web.model.ServerResponse;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(Config.Path.API_CONTEXT)
@Slf4j
public class ApiController {

    @Autowired
    private BindingRepository applicationBindingRepository;

    @Autowired
    private ApplicationLocker applicationLocker;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @RequestMapping(value = Config.Path.APPLICATIONS_SUB_PATH + "{applicationId}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteApplication(@PathVariable("applicationId") String applicationId) {
        log.debug("deleteApplication - {}", applicationId);
        applicationLocker.executeThreadSafe(applicationId,
                () -> {
                    applicationRepository.delete(applicationId);
                    applicationLocker.removeApplication(applicationId);
                    log.debug("deleteApplication - deleted");
                });

        return new ResponseEntity<>("{}", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = Config.Path.APPLICATIONS_SUB_PATH)
    @ResponseBody
    public ServerResponse<List<ApplicationInfo>> listApplications() {
        log.debug("listApplications");
        List<ApplicationInfo> result = new ArrayList<>();
        applicationRepository.findAll().forEach(result::add);
        return new ServerResponse<>(result, Instant.now());
    }

    @RequestMapping(value = Config.Path.SERVICES_SUB_PATH + "{instanceId}/applications/")
    @ResponseBody
    public ServerResponse<List<ApplicationInfo>> listApplicationsById(@PathVariable("instanceId") String
                                                                              serviceInstanceId) {
        log.debug("listApplications");
        List<ApplicationInfo> result = new ArrayList<>();
        applicationRepository.findAll()
                .forEach(app -> {
                    if (app.getEnrollmentState().getStates().keySet().contains(serviceInstanceId)) {
                        result.add(app);
                    }
                });
        return new ServerResponse<>(result, Instant.now());
    }

    @RequestMapping(Config.Path.SERVICES_SUB_PATH + "{instanceId}/bindings/")
    @ResponseBody
    public ServerResponse<List<Binding>> listBindings(@PathVariable("instanceId") String serviceInstanceId)
            throws ServiceInstanceDoesNotExistException {
        log.debug("listServiceBindings - {}", serviceInstanceId);
        List<Binding> result = new ArrayList<>();
        applicationBindingRepository.findAll()
                .forEach(serviceBinding -> {
                    if (serviceInstanceId.equals(serviceBinding.getServiceInstanceId())) {
                        result.add(serviceBinding);
                    }
                });
        return new ServerResponse<>(result, Instant.now());
    }

    @RequestMapping(Config.Path.SERVICES_SUB_PATH)
    @ResponseBody
    public ServerResponse<List<SpaceEnrollerConfig>> listInstances() {
        log.debug("listServiceInstances");
        List<SpaceEnrollerConfig> result = new ArrayList<>();
        spaceEnrollerConfigRepository.findAll().forEach(result::add);
        return new ServerResponse<>(result, Instant.now());
    }

}
