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
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/debug")
@Slf4j
public class DebugController {

    @Autowired
    private Catalog catalog;

    @RequestMapping("/applications/")
    public ModelAndView applications() {
        log.debug("applications - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", Config.Path.SERVICE_BROKER_SERVICE_CONTROLLER_BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        return new ModelAndView("views/admin/debug/applications", parameters);
    }

    @RequestMapping("/{instanceId}/bindings/")
    public ModelAndView serviceBindings(@PathVariable("instanceId") String serviceInstanceId) {
        log.debug("serviceInstances - rendering view - ", serviceInstanceId);
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", Config.Path.SERVICE_BROKER_SERVICE_CONTROLLER_BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        parameters.put("serviceInstance", serviceInstanceId);
        return new ModelAndView("views/admin/debug/bindings", parameters);
    }

    @RequestMapping("/")
    public ModelAndView serviceInstances() {
        log.debug("serviceInstances - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", Config.Path.SERVICE_BROKER_SERVICE_CONTROLLER_BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        return new ModelAndView("views/admin/debug/instances", parameters);
    }

}
