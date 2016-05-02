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

package org.cloudfoundry.autosleep.ui.proxy;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApi;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.dao.model.Binding;
import org.cloudfoundry.autosleep.access.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.worker.scheduling.TimeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Controller
@RequestMapping(Path.PROXY_CONTEXT)
@Slf4j
public class ProxyController {

    static final String HEADER_FORWARD_URL = "X-CF-Forwarded-Url";

    private static RequestEntity<?> buildOutgoingRequest(RequestEntity<?> incoming) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(incoming.getHeaders());

        URI uri = headers.remove(HEADER_FORWARD_URL).stream()
                .findFirst()
                .map(URI::create)
                .orElseThrow(() -> new IllegalStateException(String.format("No %s header present",
                        HEADER_FORWARD_URL)));

        return new RequestEntity<>(incoming.getBody(), headers, incoming.getMethod(), uri);
    }

    @Autowired
    BindingRepository bindingRepository;

    @Autowired
    CloudFoundryApi cfApi;

    @Autowired
    RestOperations restOperations;

    @Autowired
    private TimeManager timeManager;

    @RequestMapping(value = "/{routeBindingId}")
    @ResponseBody
    void badlyFormatedRequest(@PathVariable("routeBindingId") String bindingId, RequestEntity<byte[]> request) {
        log.error("Missing Header? {}", HEADER_FORWARD_URL);
        logHeader(request);
    }

    private void logHeader(RequestEntity<byte[]> request) {
        request.getHeaders()
                .toSingleValueMap()
                .forEach((name, value) -> log.debug("Header content {} - {} ", name, value));
    }

    @RequestMapping(value = "/{routeBindingId}", headers = {HEADER_FORWARD_URL})
    @ResponseBody
    ResponseEntity<?> proxify(@PathVariable("routeBindingId") String bindingId, RequestEntity<byte[]> request)
            throws CloudFoundryException, InterruptedException {

        log.debug("Incoming HTTP request for binding {} : {}", bindingId, request);

        Binding routeBinding = bindingRepository.findOne(bindingId);
        if (routeBinding == null) {
            log.debug("Route binding already removed");
            //another http request already started the app and removed the binding, just forward the traffic
        } else {
            //TODO ADD LOCK ON BINDING


            /*String route = routeBinding.getResourceId();
            Set<String> appToStart = new HashSet<>(cfApi.listRouteApplications(route));*/
            //TEMPORARY, while searching for a solution
            Set<String> appToStart = new HashSet<>(Collections.singletonList("0a9393b0-7db8-48df-8699-77543e84651a"));

            //launch start for each one
            for (Iterator<String> iter = appToStart.iterator(); iter.hasNext(); ) {
                String appId = iter.next();
                if (!cfApi.startApplication(appId)) {
                    log.debug("app {} already started", appId);
                    iter.remove();
                }
            }

            //wait for them to start
            while (!appToStart.isEmpty()) {
                timeManager.sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
                for (Iterator<String> iter = appToStart.iterator(); iter.hasNext(); ) {
                    String appId = iter.next();
                    if (CloudFoundryAppState.STARTED.equals(cfApi.getApplicationState(appId))) {
                        log.debug("app {} started", appId);
                        iter.remove();
                    } else {
                        log.debug("still waiting for app {}", appId);
                    }
                }
            }
            //TODO REMOVE LOCK
            bindingRepository.delete(bindingId);
        }
        //unqueue traffic
        RequestEntity<?> outgoing = buildOutgoingRequest(request);
        log.debug("forwarding traffic to {}", request.getHeaders().get(HEADER_FORWARD_URL));
        log.debug("Outgoing Request: {}", outgoing);

        return this.restOperations.exchange(outgoing, byte[].class);
    }

}
