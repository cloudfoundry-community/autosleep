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

import org.cloudfoundry.autosleep.Application;
import org.cloudfoundry.autosleep.access.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.cloudfoundry.autosleep.ui.proxy.ProxyController.HEADER_FORWARD_URL;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thanks to @nebhale
 * https://raw.githubusercontent.com/nebhale/route-service-example/master/src/test/java/org/cloudfoundry/example
 * /ControllerTest.java
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class ProxyControllerTest {

    private static final String BODY_VALUE = "test-body";

    @Mock
    private BindingRepository bindingRepo;

    private MockMvc mockMvc;

    /*
    static final String BINDING_ID = "someid";

    static final String ROUTE_ID = "routeid";
    */

    private MockRestServiceServer mockServer;

    @Test
    public void headRequest() throws Exception {
        this.mockServer
                .expect(method(HEAD))
                .andExpect(requestTo("http://localhost/original/head"))
                .andRespond(withSuccess());

        this.mockMvc
                .perform(head("http://localhost/" + Config.Path.PROXY_CONTEXT + "/abindingid")
                        .header(HEADER_FORWARD_URL, "http://localhost/original/head"))
                .andExpect(status().isOk());

        this.mockServer.verify();
    }

    @Test
    public void on_incoming_traffic_all_apps_stopped_linked_to_route_id_should_be_started() throws Exception {
       /*TODO?
        List<String> appList = Arrays.asList(randomUUID().toString(),

                randomUUID().toString(),
                randomUUID().toString());

        //GIVEN that the binding is known
        when(bindingRepo.findOne(BINDING_ID)).thenReturn(BeanGenerator.createRouteBinding(BINDING_ID,
                "serviceid", ROUTE_ID));
        //and that stored route is linked to a list of application
        when(cfApi.listRouteApplications(ROUTE_ID)).thenReturn(appList);
        // and application are not yet started
        when(cfApi.startApplication(anyString())).thenReturn(true);
        //And they are start very quickly (ie. first get state will return started for each of them)
        when(cfApi.getApplicationState(anyString())).thenReturn(STARTED);

        //WHEN calling proxy path THEN traffic should be forwarded without any error
        mockMvc.perform(get(Path.PROXY_CONTEXT + "/" + BINDING_ID)
                .header(ProxyController.HEADER_FORWARD_URL, "www.cloudfoundry.org")
                .accept(MediaType.ALL))
                .andExpect(status().is(HttpStatus.FOUND.value()));

        //and all apps should be started
        verify(cfApi, times(appList.size())).startApplication(anyString());
        //and slept once to let application start
        verify(timeManager, times(1)).sleep(Config.PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART);
        // and all application should be checked once
        verify(cfApi, times(appList.size())).getApplicationState(anyString());
 */
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
    public void should_fail_if_no_binding_provided() throws Exception {

        this.mockMvc
                .perform(get("http://localhost/" + Config.Path.PROXY_CONTEXT)
                        .header(HEADER_FORWARD_URL, "http://localhost/original/get"))
                .andExpect(status().isNotFound());

        this.mockServer.verify();
    }

    @Test
    public void should_forward_get_request() throws Exception {
        testForwardRequest(method(GET), "get");
    }

    public void testForwardRequest(RequestMatcher requestMatcher, String urlSuffixe) throws Exception {
        this.mockServer
                .expect(requestMatcher)
                .andExpect(requestTo("http://localhost/original/" + urlSuffixe))
                .andRespond(withSuccess(BODY_VALUE, TEXT_PLAIN));

        this.mockMvc
                .perform(get("http://localhost/" + Config.Path.PROXY_CONTEXT + "/abindingid")
                        .header(HEADER_FORWARD_URL, "http://localhost/original/" + urlSuffixe))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TEXT_PLAIN))
                .andExpect(content().string(BODY_VALUE));

        this.mockServer.verify();
    }

}