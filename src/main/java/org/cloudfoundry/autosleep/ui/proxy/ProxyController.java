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

package org.cloudfoundry.autosleep.ui.proxy;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.dao.model.Binding;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryApi;
import org.cloudfoundry.autosleep.worker.remote.CloudFoundryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Controller
@RequestMapping(Path.PROXY_CONTEXT)
@Slf4j
public class ProxyController {

    static final String HEADER_FORWARD_URL = "X-CF-Forwarded-Url";

    @Autowired
    CloudFoundryApi cfApi;

    @Autowired
    BindingRepository bindingRepository;

    @RequestMapping(value = "/{routeBindingId}")
    @ResponseBody
    public ModelAndView listApplicationsById(@PathVariable("routeBindingId") String bindingId,
                                             @RequestHeader HttpHeaders headers)
            throws CloudFoundryException, InterruptedException {


        log.debug("proxy call on route binding {}", bindingId);
        if(!headers.containsKey(HEADER_FORWARD_URL)) {
            throw new CloudFoundryException(new Exception("Missing header "+HEADER_FORWARD_URL));
        }

        Binding routeBinding = bindingRepository.findOne(bindingId);
        if (routeBinding == null) {
            log.debug("Route binding already removed");
          //another http request already started the app and removed the binding, just forward the traffic
        }else{
            //TODO ADD LOCK ON BINDING
            String routeId = routeBinding.getResourceId();

            Set<String> appToStart = new HashSet<>(cfApi.listRouteApplications(routeId));

            //launch start for each one
            for( String appId : appToStart) {
                cfApi.startApplication(appId);
            }

            //wait for them to start
            while(!appToStart.isEmpty()){
                Thread.sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART.toMillis());
                for (Iterator<String> iter = appToStart.iterator(); iter.hasNext(); ) {
                    String appId = iter.next();
                    if(CloudFoundryAppState.STARTED.equals(cfApi.getApplicationState(appId))) {
                        log.debug("app {} started",appId);
                        iter.remove();
                    } else {
                        log.debug("still waiting for app {}",appId);
                    }
                }
            }
            //TODO REMOVE LOCK
            bindingRepository.delete(bindingId);
        }
        //unqueue traffic
        log.debug("forwarding traffic to {}",headers.get(HEADER_FORWARD_URL));
        return new ModelAndView("redirect:" + headers.get(HEADER_FORWARD_URL));
    }

}
