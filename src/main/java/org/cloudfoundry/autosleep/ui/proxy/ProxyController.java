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
        //TODO ROUTE SERVICE: get matching apps with cf api, start  apps, wait, unqueue traffic.
        log.debug("route binding {}", routeBindingId);
        return null;
    }

}
