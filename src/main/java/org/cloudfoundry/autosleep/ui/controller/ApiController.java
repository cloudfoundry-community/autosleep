package org.cloudfoundry.autosleep.ui.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.ApplicationLocker;
import org.cloudfoundry.autosleep.ui.model.ServerResponse;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
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
@RequestMapping(Config.Path.apiContext)
@Slf4j
public class ApiController {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationLocker applicationLocker;


    @RequestMapping(value = Config.Path.servicesSubPath + "{instanceId}/applications/")
    @ResponseBody
    public ServerResponse<List<ApplicationInfo>> listApplicationsById(@PathVariable("instanceId") String
                                                                              serviceInstanceId) {
        log.debug("listApplications");
        List<ApplicationInfo> result = new ArrayList<>();
        applicationRepository.findAll().forEach(app -> {
            if (app.getServiceInstances().keySet().contains(serviceInstanceId)) {
                result.add(app);
            }
        });
        return new ServerResponse<>(Instant.now(), result);
    }

    @RequestMapping(Config.Path.servicesSubPath)
    @ResponseBody
    public ServerResponse<List<AutosleepServiceInstance>> listInstances() {
        log.debug("listServiceInstances");
        List<AutosleepServiceInstance> result = new ArrayList<>();
        serviceRepository.findAll().forEach(result::add);
        return new ServerResponse<>(Instant.now(), result);
    }

    @RequestMapping(Config.Path.servicesSubPath + "{instanceId}/bindings/")
    @ResponseBody
    public ServerResponse<List<ApplicationBinding>> listBindings(@PathVariable("instanceId") String serviceInstanceId)
            throws ServiceInstanceDoesNotExistException {
        log.debug("listServiceBindings - {}", serviceInstanceId);
        List<ApplicationBinding> result = new ArrayList<>();
        bindingRepository.findAll().forEach(serviceBinding -> {
                    if (serviceInstanceId.equals(serviceBinding.getServiceInstanceId())) {
                        result.add(serviceBinding);
                    }
                }
        );
        return new ServerResponse<>(Instant.now(), result);
    }

    @RequestMapping(value = Config.Path.applicationsSubPath)
    @ResponseBody
    public ServerResponse<List<ApplicationInfo>> listApplications() {
        log.debug("listApplications");
        List<ApplicationInfo> result = new ArrayList<>();
        applicationRepository.findAll().forEach(result::add);
        return new ServerResponse<>(Instant.now(), result);
    }


    @RequestMapping(value = Config.Path.applicationsSubPath + "{applicationId}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteApplication(@PathVariable("applicationId") String applicationId) {
        log.debug("deleteApplication - {}", applicationId);
        applicationLocker.executeThreadSafe(applicationId, () -> {
            applicationRepository.delete(applicationId);
            applicationLocker.removeApplication(applicationId);
            log.debug("deleteApplication - deleted");
        });

        return new ResponseEntity<>("{}", HttpStatus.NO_CONTENT);
    }

}
