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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    String proxySignature;

    @Autowired
    private CloudFoundryApiService cfApi;

    @Autowired
    private Environment env;

    @Autowired
    private ProxyMapEntryRepository proxyMap;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TimeManager timeManager;

    private RequestEntity<?> getOutgoingRequest(RequestEntity<?> incoming, URI destination) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(incoming.getHeaders());
        //add custom header with our signature, to identify our own forwarded traffic
        headers.put(HEADER_FORWARDED, Collections.singletonList(proxySignature));
        return new RequestEntity<>(incoming.getBody(), headers, incoming.getMethod(), destination);
    }

    @ExceptionHandler(CloudFoundryException.class)
    ResponseEntity<String> handleCloudfoundryException(CloudFoundryException error) {
        log.error("cloudfoundry error", error);
        return new ResponseEntity<>("Error while calling remote api", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InterruptedException.class)
    ResponseEntity<String> handleCloudfoundryException(InterruptedException error) {
        return new ResponseEntity<>("Internal server error: " + error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostConstruct
    void init() throws UnknownHostException, NoSuchAlgorithmException, UnsupportedEncodingException {
        //not stored in Config, because this impl is temporary
        String securityPass = env.getProperty("security.user.password");
        String autosleepHost = InetAddress.getLocalHost().getHostName();
        this.proxySignature = Arrays.toString(MessageDigest.getInstance("MD5")
                .digest((autosleepHost + securityPass).getBytes("UTF-8")));

    }

    @RequestMapping(headers = {HEADER_PROTOCOL, HEADER_HOST})
    ResponseEntity<?> proxify(@RequestHeader(HEADER_HOST) String targetHost,
                              RequestEntity<byte[]> incoming,
                              HttpServletRequest request) throws InterruptedException, CloudFoundryException {

        List<String> alreadyForwardedHeader = incoming.getHeaders().get(HEADER_FORWARDED);
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        log.debug("Incoming Request for route : {} path: {}", targetHost, path);

        if (alreadyForwardedHeader != null && proxySignature.equals(alreadyForwardedHeader.get(0))) {
            log.error("We've already forwarded this traffic, this should not happen");
            return new ResponseEntity<>("Infinite loop forwarding error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ProxyMapEntry mapEntry = proxyMap.findOne(targetHost);

        if (mapEntry == null) {
            log.debug("No enrolled application associated with route : {}", targetHost);
            return new ResponseEntity<>("Sorry, but this page doesn't exist! ", HttpStatus.NOT_FOUND);
        }

        String appId = mapEntry.getAppId();

        String applicationState = cfApi.getApplicationState(appId);
        if (CloudFoundryAppState.STARTED.equals(applicationState) && !cfApi.isAppRunning(appId)) {
            log.info("Rejecting traffic for starting app [{}]", appId);
            return new ResponseEntity<>("The app is starting, please retry in few seconds", HttpStatus
                    .SERVICE_UNAVAILABLE);
        } else if (CloudFoundryAppState.STOPPED.equals(applicationState)) {
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
        proxyMap.deleteAppRoutesIfExists(appId);
        String protocol = incoming.getHeaders().get(HEADER_PROTOCOL).get(0);
        URI uri = URI.create(protocol + "://" + targetHost + path);
        RequestEntity<?> outgoing = getOutgoingRequest(incoming, uri);
        log.debug("Outgoing Request: {}", outgoing);

        //if "outgoing" point to a 404, this will trigger a 500. Is this really a pb?
        return this.restTemplate.exchange(outgoing, byte[].class);

    }

}
