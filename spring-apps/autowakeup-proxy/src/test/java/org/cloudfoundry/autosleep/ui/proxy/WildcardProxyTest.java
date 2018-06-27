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

import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.dao.model.ProxyMapEntry;
import org.cloudfoundry.autosleep.access.dao.repositories.ProxyMapEntryRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.util.TimeManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.cloudfoundry.autosleep.ui.proxy.WildcardProxy.HEADER_FORWARDED;
import static org.cloudfoundry.autosleep.ui.proxy.WildcardProxy.HEADER_HOST;
import static org.cloudfoundry.autosleep.ui.proxy.WildcardProxy.HEADER_PROTOCOL;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = WildcardProxyTest.Configuration.class)
@WebAppConfiguration
public class WildcardProxyTest {

    @org.springframework.context.annotation.Configuration
    @ComponentScan(basePackages = {"org.cloudfoundry.autosleep.ui.proxy"}, includeFilters = @Filter(Controller.class),
            excludeFilters = @Filter(org.springframework.context.annotation.Configuration.class))
    public static class Configuration {

        @Bean
        CloudFoundryApiService cfApi() {
            return mock(CloudFoundryApiService.class);
        }

        @Bean
        ProxyMapEntryRepository proxyMap() {
            return mock(ProxyMapEntryRepository.class);
        }

        @Bean
        RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        TimeManager timeManager() {
            return mock(TimeManager.class);
        }

    }

    private static final String APP_ID = "test-app-id";

    private static final String BODY_VALUE = "test-body";

    private static final String HOST_TEST_VALUE = "test-host";

    private static final String PROTOCOL_TEST_VALUE = "http";

    @Autowired
    private CloudFoundryApiService cfApi;

    private MockMvc mockMvc;

    @Autowired
    private WildcardProxy proxy;

    @Autowired
    private ProxyMapEntryRepository proxyMap;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TimeManager timeManager;

    @Before
    public void init() {
        reset(timeManager, proxyMap, cfApi, restTemplate);
        this.mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .build();
    }

    private void mockRemoteExchange(HttpStatus statusCode, MediaType contentType, String body) {
        when(restTemplate.exchange(any(RequestEntity.class), eq(byte[].class)))
                .then(invocation -> {
                    RequestEntity<?> requestEntity = (RequestEntity) invocation.getArguments()[0];
                    assertTrue(requestEntity.getUrl().toString()
                            .startsWith(PROTOCOL_TEST_VALUE + "://" + HOST_TEST_VALUE));
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(contentType);

                    return new ResponseEntity<>(body, headers, statusCode);
                });
    }

    @Test
    public void should_forward_traffic_if_application_restarted() throws Exception {
        //GIVEN that we have a map route in database (for stopped app)
        when(proxyMap.findOne(HOST_TEST_VALUE)).thenReturn(ProxyMapEntry.builder()
                .appId(APP_ID)
                .host(HOST_TEST_VALUE)
                .build());
        when(cfApi.getApplicationState(APP_ID)).thenReturn(CloudFoundryAppState.STARTED);
        // is app running will return false the two first times
        when(cfApi.isAppRunning(APP_ID)).thenReturn(true);
        //the return body will return the expected body
        mockRemoteExchange(HttpStatus.OK, MediaType.TEXT_PLAIN, BODY_VALUE);

        //WHEN an incoming message target this same route
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE))
                //then status code is ok
                .andExpect(status().isOk())
                //and the content type remains the same
                .andExpect(content().contentType(TEXT_PLAIN))
                // and the body is correct
                .andExpect(content().string(BODY_VALUE));

        // and start was not called
        verify(cfApi, never()).startApplication(APP_ID);
        // and we never wait for anything
        verify(timeManager, never()).sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
        //and we removed the application from repository
        verify(proxyMap, times(1)).deleteAppRoutesIfExists(APP_ID);
    }

    @Test
    public void should_send_404_if_not_in_route_map() throws Exception {
        /*this.mockServer
                .expect(method(GET))
                .andExpect(requestTo("http://localhost/original/get"))
                .andExpect(header(HEADER_HOST, HOST_TEST_VALUE))
                .andExpect(header(HEADER_PROTOCOL, "http"))
                .andRespond(withSuccess(BODY_VALUE, TEXT_PLAIN));

                 .andExpect(content().contentType(TEXT_PLAIN))
                .andExpect(content().string(BODY_VALUE));*/

        //GIVEN that no route map is stored in database
        when(proxyMap.findOne(HOST_TEST_VALUE)).thenReturn(null);
        //WHEN an incoming message contains an unknown route
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_send_internal_error_if_already_signed_by_proxy() throws Exception {
        //GIVEN
        //WHEN an incoming message contains the signature header with our own signature
        //THEN return 500 error (as this should not happen)
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE)
                        .header(HEADER_FORWARDED, proxy.proxySignature))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void should_send_internal_error_on_remote_api_error() throws Exception {
        //GIVEN that we have a map route in database (for started app)
        when(proxyMap.findOne(HOST_TEST_VALUE)).thenReturn(ProxyMapEntry.builder()
                .appId(APP_ID)
                .host(HOST_TEST_VALUE)
                .build());
        when(cfApi.getApplicationState(APP_ID)).thenThrow(CloudFoundryException.class);
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE)
                        .header(HEADER_FORWARDED, proxy.proxySignature))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void should_send_service_unavailable_if_application_is_restarting() throws Exception {
        //GIVEN that we have a map route in database (for started app)
        when(proxyMap.findOne(HOST_TEST_VALUE)).thenReturn(ProxyMapEntry.builder()
                .appId(APP_ID)
                .host(HOST_TEST_VALUE)
                .build());
        when(cfApi.getApplicationState(APP_ID)).thenReturn(CloudFoundryAppState.STARTED);
        // is app running returns true
        when(cfApi.isAppRunning(APP_ID)).thenReturn(false);

        //WHEN an incoming message target this same route
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE))
                //then status code is ok
                .andExpect(status().isServiceUnavailable());

        verify(proxyMap, never()).deleteIfExists(HOST_TEST_VALUE);
    }

    @Test
    public void should_start_a_stopped_application_and_return_the_body() throws Exception {
        //GIVEN that we have a map route in database (for stopped app)
        when(proxyMap.findOne(HOST_TEST_VALUE)).thenReturn(ProxyMapEntry.builder()
                .appId(APP_ID)
                .host(HOST_TEST_VALUE)
                .build());
        when(cfApi.getApplicationState(APP_ID)).thenReturn(CloudFoundryAppState.STOPPED);
        // is app running will return false the two first times
        when(cfApi.isAppRunning(APP_ID)).thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);
        //the return body will return the expected body
        mockRemoteExchange(HttpStatus.OK, MediaType.TEXT_PLAIN, BODY_VALUE);

        //WHEN an incoming message target this same route
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE))
                //then status code is ok
                .andExpect(status().isOk())
                //and the content type remains the same
                .andExpect(content().contentType(TEXT_PLAIN))
                // and the body is correct
                .andExpect(content().string(BODY_VALUE));

        // and start was called
        verify(cfApi, times(1)).startApplication(APP_ID);
        verify(timeManager, times(3)).sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
        //and we removed the application from repository
        verify(proxyMap, times(1)).deleteAppRoutesIfExists(APP_ID);
    }

}
