package org.cloudfoundry.autosleep.ui.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceController;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
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

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @RequestMapping("/")
    public ModelAndView serviceInstances() {
        log.debug("serviceInstances - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        return new ModelAndView("views/admin/debug/instances", parameters);
    }

    @RequestMapping("/{instanceId}/bindings/")
    public ModelAndView serviceBindings(@PathVariable("instanceId") String serviceInstanceId) {
        log.debug("serviceInstances - rendering view - ", serviceInstanceId);
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        parameters.put("serviceInstance", serviceInstanceId);
        return new ModelAndView("views/admin/debug/bindings", parameters);
    }


    @RequestMapping("/applications/")
    public ModelAndView applications() {
        log.debug("applications - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        parameters.put("skipNavigation", false);
        return new ModelAndView("views/applications", parameters);
    }



}
