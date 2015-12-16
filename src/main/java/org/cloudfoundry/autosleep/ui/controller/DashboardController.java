package org.cloudfoundry.autosleep.ui.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceController;
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
    private ServiceRepository serviceRepository;

    @ExceptionHandler({ServiceInstanceDoesNotExistException.class})
    @ResponseBody
    public String handleException(ServiceInstanceDoesNotExistException ex,
                                                        HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.TEXT_HTML.toString());
        response.setCharacterEncoding("UTF-8");
        return ex.getMessage();
    }


    @RequestMapping("/{serviceInstanceId}")
    public ModelAndView appForService(@PathVariable("serviceInstanceId") String serviceInstanceId)
            throws ServiceInstanceDoesNotExistException {
        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceInstanceId);
        if (serviceInstance != null) {
            log.debug("appForService - rendering view");

            Map<String, Object> parameters = new HashMap<>();
            ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
            parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
            parameters.put("serviceDefinitionId", serviceDefinition.getId());
            parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
            parameters.put("serviceInstance", serviceInstanceId);
            parameters.put("skipNavigation", true);

            parameters.put("forcedAutoEnrollment", serviceInstance.isForcedAutoEnrollment());
            parameters.put("idleDuration", serviceInstance.getIdleDuration().toString());
            if (serviceInstance.getExcludeFromAutoEnrollment() != null) {
                parameters.put("excludeFromAutoEnrollment",
                        serviceInstance.getExcludeFromAutoEnrollment().pattern());
            } else {
                parameters.put("excludeFromAutoEnrollment", "none");
            }
            return new ModelAndView("views/applications", parameters);
        } else {
            log.debug("appForService - service {} not found", serviceInstanceId);
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }
    }
}
