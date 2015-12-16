package org.cloudfoundry.integrationclient;/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.spring.SpringCloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationStatisticsRequest;
import org.cloudfoundry.client.v2.applications.ApplicationStatisticsResponse;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.rx.Streams;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by buce8373 on 08/12/2015.
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Sandbox.SandboxConfiguration.class)
public class Sandbox {

    @PropertySource("classpath:test.properties")
    public static class SandboxConfiguration {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Value("${cf.url}")
    private String cfUrl;

    @Value("${cf.client.id}")
    private String cfClientId;

    @Value("${cf.client.secret}")
    private String cfClientSecret;

    @Value("${cf.username}")
    private String cfUsername;

    @Value("${cf.password}")
    private String cfPassword;

    @Value("${cf.skip.verification}")
    private boolean skipVerification;

    @Value("${test.application.id}")
    private String applicationId;

    @Value("${test.space.id}")
    private String spaceId;

    @Value("${test.service.instance.id}")
    private String serviceInstanceId;


    private CloudFoundryClient client;


    @Before
    public void buildClient() {
        if (client == null) {
            log.debug("Building to {}", cfUrl);
            client = SpringCloudFoundryClient.builder()
                    .host(cfUrl)
                    .clientId(cfClientId)
                    .clientSecret(cfClientSecret)
                    .skipSslValidation(skipVerification)
                    .username(cfUsername)
                    .password(cfPassword).build();

        }
    }

    @Test
    public void get_application() {
        Publisher<GetApplicationResponse> publisher = this.client
                .applicationsV2().get(GetApplicationRequest.builder().id(applicationId).build());
        GetApplicationResponse response = Streams.wrap(publisher).next().get();
        assertThat(response.getMetadata(), is(notNullValue()));
        assertThat(response.getEntity(), is(notNullValue()));
        assertThat(response.getMetadata().getId(), is(equalTo(applicationId)));
        log.debug("{} found", response.getEntity().getName());

    }



    @Test
    public void test_stop() {
        log.debug("test_stop - start");
        log.debug("Stopping application {}", applicationId);
        Publisher<UpdateApplicationResponse> publisherStart = client.applicationsV2().update
                (UpdateApplicationRequest.builder().id(applicationId).state("STOPPED").build());
        UpdateApplicationResponse responseStart = Streams.wrap(publisherStart).next().get();
        log.debug("update response - {}", responseStart);
        log.debug("test_stop - end");
    }

    @Test
    public void test_start() {
        log.debug("test_start - start");
        log.debug("Starting application {}", applicationId);
        Publisher<UpdateApplicationResponse> publisherStart = client.applicationsV2().update
                (UpdateApplicationRequest.builder().id(applicationId).state("STARTED").build());
        UpdateApplicationResponse responseStart = Streams.wrap(publisherStart).next().get();
        log.debug("update response - {}", responseStart);
        log.debug("test_stop - end");
    }


    @Test
    public void test_list_by_space() {
        Publisher<ListApplicationsResponse> publisher = this.client
                .applicationsV2()
                .list(org.cloudfoundry.client.v2.applications.ListApplicationsRequest.builder()
                        .spaceId(spaceId).build());
        ListApplicationsResponse response = Streams.wrap(publisher).next().get();
        assertThat(response.getResources(), is(notNullValue()));
        response.getResources().stream()
                .map(applicationResource -> applicationResource.getEntity().getName())
                .forEach(log::debug);
    }

    @Test
    public void get_last_logs() {
        throw new RuntimeException("Not yet implemented");
    }

    @Test
    public void get_last_events() {
        throw new RuntimeException("Not yet implemented");
    }

    @Test
    public void test_get_service_instance() {
        throw new RuntimeException("Not yet implemented");
    }

    @Test
    public void test_bind_application() {
        throw new RuntimeException("Not yet implemented");
    }





}
