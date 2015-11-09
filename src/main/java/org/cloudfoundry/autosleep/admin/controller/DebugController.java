package org.cloudfoundry.autosleep.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.admin.model.ServiceBinding;
import org.cloudfoundry.autosleep.admin.model.ServiceInstance;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.community.servicebroker.controller.ServiceInstanceController;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/debug")
@Slf4j
public class DebugController {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @RequestMapping("/")
    public ModelAndView serviceInstances() {
        log.debug("serviceInstances - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalogService.getCatalog().getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        return new ModelAndView("views/admin/debug/service_instances", parameters);
    }

    @RequestMapping("/{instanceId}/bindings/")
    public ModelAndView serviceBindings(@PathVariable("instanceId") String serviceInstanceId) {
        log.debug("serviceInstances - rendering view - ", serviceInstanceId);
        Map<String, Object> parameters = new HashMap<>();
        ServiceDefinition serviceDefinition = catalogService.getCatalog().getServiceDefinitions().get(0);
        parameters.put("pathServiceInstances", ServiceInstanceController.BASE_PATH);
        parameters.put("serviceDefinitionId", serviceDefinition.getId());
        parameters.put("planId", serviceDefinition.getPlans().get(0).getId());
        parameters.put("serviceInstance", serviceInstanceId);
        return new ModelAndView("views/admin/debug/service_bindings", parameters);
    }


    @RequestMapping("/services/servicesinstances/")
    @ResponseBody
    public List<ServiceInstance> listServiceInstances() {
        log.debug("listServiceInstances");
        List<ServiceInstance> result = new ArrayList<>();
        serviceRepository.findAll().forEach(service ->
                result.add(new ServiceInstance(service.getServiceInstanceId(),
                        service.getServiceDefinitionId(), service.getPlanId())));
        return result;
    }

    @RequestMapping("/services/servicebindings/{instanceId}")
    @ResponseBody
    public List<ServiceBinding> listServiceBindings(@PathVariable("instanceId") String serviceInstanceId)
            throws ServiceInstanceDoesNotExistException {
        log.debug("listServiceBindings - {}", serviceInstanceId);
        List<ServiceBinding> result = new ArrayList<>();
        bindingRepository.findAll().forEach(serviceBinding -> {
                    if (serviceInstanceId.equals(serviceBinding.getServiceInstanceId())) {
                        result.add(new ServiceBinding(serviceBinding.getAppGuid(), serviceBinding.getId()));
                    }
                }
        );
        return result;
    }


}
