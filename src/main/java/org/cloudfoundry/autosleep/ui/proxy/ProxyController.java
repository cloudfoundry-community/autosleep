package org.cloudfoundry.autosleep.ui.proxy;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.ui.web.model.ServerResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(Path.PROXY_CONTEXT)
@Slf4j
public class ProxyController {

    @RequestMapping(value = "/{routeBindingId}")
    @ResponseBody
    public ServerResponse<List<ApplicationInfo>> listApplicationsById(@PathVariable("routeBindingId") String
                                                                              routeBindingId) {
        //TODO ROUTE SERVICE: start matching app, wait, unqueue traffic.
        log.debug("route binding {}", routeBindingId);
        return null;
    }

}
