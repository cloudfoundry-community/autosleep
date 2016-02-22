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

package org.cloudfoundry.autosleep.ui.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(Config.Path.DASHBOARD_CONTEXT)
@Slf4j
public class DashboardController {

    @Autowired
    private Catalog catalog;

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @RequestMapping("/{serviceInstanceId}")
    public ModelAndView appForService(@PathVariable("serviceInstanceId") String serviceInstanceId)
            throws ServiceInstanceDoesNotExistException {
        SpaceEnrollerConfig serviceInstance = spaceEnrollerConfigRepository.findOne(serviceInstanceId);
        if (serviceInstance != null) {
            log.debug("appForService - rendering view");

            Map<String, Object> parameters = new HashMap<>();
            ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
            parameters.put("pathServiceInstances", Config.Path.SERVICE_BROKER_SERVICE_CONTROLLER_BASE_PATH);
            parameters.put("serviceDefinitionId", serviceDefinition.getId());
            parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
            parameters.put("serviceInstance", serviceInstanceId);

            parameters.put("forcedAutoEnrollment", serviceInstance.isForcedAutoEnrollment());
            parameters.put("idleDuration", serviceInstance.getIdleDuration().toString());
            if (serviceInstance.getExcludeFromAutoEnrollment() != null) {
                parameters.put("excludeFromAutoEnrollment",
                        serviceInstance.getExcludeFromAutoEnrollment().pattern());
            } else {
                parameters.put("excludeFromAutoEnrollment", "none");
            }
            return new ModelAndView("views/dashboard", parameters);
        } else {
            log.debug("appForService - service {} not found", serviceInstanceId);
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }
    }

    @ExceptionHandler({ServiceInstanceDoesNotExistException.class})
    @ResponseBody
    public String handleException(ServiceInstanceDoesNotExistException ex,
                                  HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.TEXT_HTML.toString());
        response.setCharacterEncoding("UTF-8");
        return ex.getMessage();
    }

}
