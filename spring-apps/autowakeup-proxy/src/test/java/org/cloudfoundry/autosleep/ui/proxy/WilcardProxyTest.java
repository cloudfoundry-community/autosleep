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

import org.cloudfoundry.autosleep.access.dao.model.ProxyMapEntry;
import org.cloudfoundry.autosleep.access.dao.repositories.ProxyMapEntryRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.cloudfoundry.autosleep.ui.proxy.WildcardProxy.HEADER_FORWARDED;
import static org.cloudfoundry.autosleep.ui.proxy.WildcardProxy.HEADER_HOST;
import static org.cloudfoundry.autosleep.ui.proxy.WildcardProxy.HEADER_PROTOCOL;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class WilcardProxyTest {

    private static final String BODY_VALUE = "test-body";

    private static final String HOST_TEST_VALUE = "test-host";

    private static final String PROTOCOL_TEST_VALUE = "http";

    @Autowired
    WildcardProxy proxy;

    @Autowired
    ProxyMapEntryRepository routeMap;

    private MockMvc mockMvc;

    private MockRestServiceServer mockServer;

    @Before
    public void clearDatabase() {
        routeMap.deleteAll();
    }

    @Autowired
    void setRestTemplate(RestTemplate restTemplate) {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Autowired
    void setWebApplicationContext(WebApplicationContext wac) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void should_forward_traffic_if_in_route_map() throws Exception {
        //GIVEN that we have a map route in database (for an app which restart isn't in progress
        routeMap.save(ProxyMapEntry.builder().host(HOST_TEST_VALUE)
                .appId("ANYAPPID")
                .isRestarting(false).build());

        this.mockServer
                .expect(method(GET))
                .andExpect(requestTo(PROTOCOL_TEST_VALUE + "://" + HOST_TEST_VALUE + "/anything"))
                .andExpect(header(HEADER_HOST, HOST_TEST_VALUE))
                .andExpect(header(HEADER_PROTOCOL, "http"))
                .andRespond(withSuccess(BODY_VALUE, TEXT_PLAIN));

        //WHEN an incoming message target this same route
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TEXT_PLAIN))
                .andExpect(content().string(BODY_VALUE));

        //THEN the message is forwarded
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

        //WHEN an incoming message contains an unknown route
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE))
                .andExpect(status().isNotFound());

        //THEN return a 404
        this.mockServer.verify();
    }

    @Test
    public void should_send_500_if_already_signed_by_proxy() throws Exception {
        //GIVEN
        //WHEN an incoming message contains the signature header with our own signature
        //THEN return 500 error (as this should not happen)
        this.mockMvc
                .perform(get("http://localhost/anything")
                        .header(HEADER_HOST, HOST_TEST_VALUE)
                        .header(HEADER_PROTOCOL, PROTOCOL_TEST_VALUE)
                        .header(HEADER_FORWARDED, proxy.proxySignature))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

}
