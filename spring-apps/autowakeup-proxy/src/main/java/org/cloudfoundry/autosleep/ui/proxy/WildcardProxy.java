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
import org.cloudfoundry.autosleep.access.dao.model.ProxyMapEntry;
import org.cloudfoundry.autosleep.access.dao.repositories.ProxyMapEntryRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.util.TimeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
@Transactional
public class WildcardProxy {

    static final String HEADER_FORWARDED = "CF-Autosleep-Proxy-Signature";

    static final String HEADER_HOST = "host";

    static final String HEADER_PROTOCOL = "x-forwarded-proto";

    private final RestTemplate restTemplate;

    protected String proxySignature;

    @Autowired
    private CloudFoundryApi cfApi;

    @Autowired
    private Environment env;

    @Autowired
    private ProxyMapEntryRepository proxyMap;

    @Autowired
    private TimeManager timeManager;

    @Autowired
    WildcardProxy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private RequestEntity<?> getOutgoingRequest(RequestEntity<?> incoming, URI destination) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(incoming.getHeaders());
        //add custom header with our signature, to identify our own forwarded traffic
        headers.put(HEADER_FORWARDED, Collections.singletonList(proxySignature));
        return new RequestEntity<>(incoming.getBody(), headers, incoming.getMethod(), destination);
    }

    @PostConstruct
    void init() throws Exception {
        //not stored in Config, because this impl is temporary
        String securityPass = env.getProperty("security.user.password");
        String autosleepHost = InetAddress.getLocalHost().getHostName();
        this.proxySignature = Arrays.toString(MessageDigest.getInstance("MD5").digest((autosleepHost
                + securityPass).getBytes("UTF-8")));

    }

    @RequestMapping(headers = {HEADER_PROTOCOL, HEADER_HOST})
    ResponseEntity<?> proxify(@RequestHeader(HEADER_HOST) String targetHost,
                              RequestEntity<byte[]> incoming,
                              HttpServletRequest request) throws InterruptedException {

        List<String> alreadyForwardedHeader = incoming.getHeaders().get(HEADER_FORWARDED);
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        log.debug("Incoming Request for route : {} path: {}", targetHost, path);

        if (alreadyForwardedHeader != null && proxySignature.equals(alreadyForwardedHeader.get(0))) {
            log.error("We've already forwarded this traffic, this should not happen");
            return new ResponseEntity<>("Infinite loop forwarding error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ProxyMapEntry mapEntry = proxyMap.findOne(targetHost);

        if (mapEntry == null) {
            return new ResponseEntity<>("Sorry, but this page doesn't exist! ", HttpStatus.NOT_FOUND);
        }

        try {
            String appId = mapEntry.getAppId();

            if (CloudFoundryAppState.STARTED.equals(cfApi.getApplicationState(appId)) && !cfApi.isAppRunning(appId)) {
                return new ResponseEntity<>("Autosleep is restarting, please retry in few seconds", HttpStatus
                        .SERVICE_UNAVAILABLE);
            }

            if (CloudFoundryAppState.STOPPED.equals(cfApi.getApplicationState(appId))) {
                log.info("Starting app [{}]", appId);
                cfApi.startApplication(appId);
                timeManager.sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
            }
            while (!cfApi.isAppRunning(appId)) {
                log.debug("waiting for app {} restart...", appId);
                timeManager.sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
                //TODO add timeout that would log error and reset mapEntry.isStarting to false
            }
            //if exist, to prevent exception when two instances started the app in //
            proxyMap.deleteIfExists(mapEntry.getHost());

        } catch (CloudFoundryException e) {
            log.error("Couldn't launch app restart", e);
        }

        String protocol = incoming.getHeaders().get(HEADER_PROTOCOL).get(0);
        URI uri = URI.create(protocol + "://" + targetHost + path);
        RequestEntity<?> outgoing = getOutgoingRequest(incoming, uri);
        log.debug("Outgoing Request: {}", outgoing);

        //if "outgoing" point to a 404, this will trigger a 500. Is this really a pb?
        return this.restTemplate.exchange(outgoing, byte[].class);
    }

}
