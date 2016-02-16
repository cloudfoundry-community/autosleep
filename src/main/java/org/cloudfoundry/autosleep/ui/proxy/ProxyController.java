package org.cloudfoundry.autosleep.ui.proxy;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.ui.web.model.ServerResponse;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(Path.PROXY_CONTEXT)
@Slf4j
public class ProxyController {

    @Autowired
    private SpaceEnrollerConfigRepository spaceEnrollerConfigRepository;

    @Autowired
    private ApplicationBindingRepository applicationBindingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationLocker applicationLocker;


    @RequestMapping(value = "/{routeBindingId}")
    @ResponseBody
    public ServerResponse<List<ApplicationInfo>> listApplicationsById(@PathVariable("routeBindingId") String
                                                                              routeBindingId) {
       //TODO ROUTE SERVICE: start matching app, wait, unqueue traffic.
        return null;
    }

}
