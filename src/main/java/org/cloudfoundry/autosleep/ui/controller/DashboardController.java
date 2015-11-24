package org.cloudfoundry.autosleep.ui.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
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
@RequestMapping(Config.Path.dashboardPrefix)
@Slf4j
public class DashboardController {

    @Autowired
    private Catalog catalog;

    @Autowired
    private ServiceRepository serviceRepository;

    @RequestMapping("/{serviceInstanceId}")
    public ModelAndView appForService(@PathVariable("serviceInstanceId") String serviceInstanceId) {
        log.debug("appForService - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        parameters.put("serviceInstance", serviceInstanceId);
        parameters.put("skipNavigation", true);

        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceInstanceId);
        if (serviceInstance != null) {
            parameters.put("noOptout",serviceInstance.isNoOptOut());
            parameters.put("interval",serviceInstance.getInterval().toString());
            if (serviceInstance.getExcludeNames() != null) {
                parameters.put("excludeNames",serviceInstance.getExcludeNames().toString());
            } else {
                parameters.put("excludeNames","none");
            }
        }

        return new ModelAndView("views/applications", parameters);

    }


}
