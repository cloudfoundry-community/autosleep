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
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.config.Config.Path;
import org.cloudfoundry.autosleep.util.TimeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

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
    CloudFoundryApiService cfApi;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private TimeManager timeManager;

    @RequestMapping(value = "/{appGuidToRestart}", headers = {HEADER_FORWARD_URL})
    @ResponseBody
    ResponseEntity<?> proxify(@PathVariable("appGuidToRestart") String appId, RequestEntity<byte[]> request)
            throws CloudFoundryException, InterruptedException {

        log.debug("Incoming HTTP request for app {} : {}", appId, request);

        if (!CloudFoundryAppState.STARTED.equals(cfApi.getApplicationState(appId))) {
            cfApi.startApplication(appId);
            timeManager.sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
        }
        while (!CloudFoundryAppState.STARTED.equals(cfApi.getApplicationState(appId))) {
            log.debug("waiting for app {} restart...", appId);
            timeManager.sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
            //TODO add timeout that would log error and reset semaphore?
        }

        //unqueue traffic
        RequestEntity<?> outgoing = buildOutgoingRequest(request);
        log.debug("forwarding traffic to {}", request.getHeaders().get(HEADER_FORWARD_URL));
        log.debug("Outgoing Request: {}", outgoing);

        return this.restTemplate.exchange(outgoing, byte[].class);
    }

}
