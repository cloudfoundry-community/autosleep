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
import org.cloudfoundry.client.LoggregatorClient;
import org.cloudfoundry.client.loggregator.LoggregatorMessage;
import org.cloudfoundry.client.loggregator.RecentLogsRequest;
import org.cloudfoundry.client.spring.SpringCloudFoundryClient;
import org.cloudfoundry.client.spring.SpringLoggregatorClient;
import org.cloudfoundry.client.v2.applications.*;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.servicebindings.*;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.utils.test.TestSubscriber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by buce8373 on 08/12/2015.
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Sandbox.SandboxConfiguration.class)
public class Sandbox {

    private static final long DEFAULT_TIMEOUT_IN_SECONDS = 2;

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

    private LoggregatorClient loggregatorClient;

    @Before
    public void buildClient() {
        if (client == null) {
            log.debug("Building to {}", cfUrl);
            SpringCloudFoundryClient client = SpringCloudFoundryClient.builder()
                    .host(cfUrl)
                    .clientId(cfClientId)
                    .clientSecret(cfClientSecret)
                    .skipSslValidation(skipVerification)
                    .username(cfUsername)
                    .password(cfPassword).build();
            loggregatorClient = SpringLoggregatorClient.builder().cloudFoundryClient(client).build();
            this.client = client;
        }
    }

    @Test
    public void get_application_poll() {
        log.debug("get_application_get");
        Mono<GetApplicationResponse> publisher = this.client
                .applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationId).build());
        //wait for 30s by default
        GetApplicationResponse response = publisher.get();
        assertThat(response, is(notNullValue()));
        assertThat(response.getMetadata(), is(notNullValue()));
        assertThat(response.getEntity(), is(notNullValue()));
        assertThat(response.getMetadata().getId(), is(equalTo(applicationId)));
        log.debug("get_application_get - {} found", response.getEntity().getName());

    }

    @Test
    public void get_application_subscribe() throws InterruptedException {
        log.debug("get_application_subscribe");
        TestSubscriber<GetApplicationResponse> subscriber = new TestSubscriber<>();

        subscriber.assertThat(response -> {
            assertThat(response, is(notNullValue()));
            assertThat(response.getMetadata(), is(notNullValue()));
            assertThat(response.getEntity(), is(notNullValue()));
            assertThat(response.getMetadata().getId(), is(equalTo(applicationId)));
            log.debug("get_application_subscribe - {} found", response.getEntity().getName());
        });
        Mono<GetApplicationResponse> publisher = this.client
                .applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationId).build());
        publisher.subscribe(subscriber);
        subscriber.verify(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

    }

    @Test
    public void test_stop() throws InterruptedException {
        log.debug("test_stop - start");
        log.debug("Stopping application {}", applicationId);
        TestSubscriber<UpdateApplicationResponse> subscriber = new TestSubscriber<>();
        subscriber.assertThat(response -> {
            assertThat(response, is(notNullValue()));
            assertThat(response.getMetadata(), is(notNullValue()));
            assertThat(response.getMetadata().getId(), is(equalTo(applicationId)));
            log.debug("application stopped - {}", response.getEntity().getName());
            log.debug("test_stop - end");
        });
        Mono<UpdateApplicationResponse> publisherStart = client.applicationsV2().update
                (UpdateApplicationRequest.builder().applicationId(applicationId).state("STOPPED").build());
        publisherStart.subscribe(subscriber);
        subscriber.verify(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void test_start() throws InterruptedException {
        log.debug("test_start - start");
        log.debug("Starting application {}", applicationId);
        TestSubscriber<UpdateApplicationResponse> subscriber = new TestSubscriber<>();
        subscriber.assertThat(response -> {
            assertThat(response, is(notNullValue()));
            assertThat(response.getMetadata(), is(notNullValue()));
            assertThat(response.getMetadata().getId(), is(equalTo(applicationId)));
            log.debug("application started - {}", response.getEntity().getName());
            log.debug("test_stop - end");
        });
        Mono<UpdateApplicationResponse> publisherStart = client.applicationsV2().update
                (UpdateApplicationRequest.builder().applicationId(applicationId).state("STARTED").build());
        publisherStart.subscribe(subscriber);
        subscriber.verify(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void test_list_by_space() throws InterruptedException {
        TestSubscriber<ListApplicationsResponse> subscriber = new TestSubscriber<>();
        subscriber.assertThat(response -> {
            assertThat(response, is(notNullValue()));
            assertThat(response.getResources(), is(notNullValue()));
            response.getResources().stream()
                    .map(applicationResource -> applicationResource.getEntity().getName())
                    .forEach(log::debug);
        });

        Mono<ListApplicationsResponse> publisher = this.client
                .applicationsV2()
                .list(org.cloudfoundry.client.v2.applications.ListApplicationsRequest.builder()
                        .spaceId(spaceId).build());
        publisher.subscribe(subscriber);
        subscriber.verify(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void get_last_logs() throws Throwable {
        final AtomicInteger count = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);
        final AtomicLong newestTimestamp = new AtomicLong();
        Subscriber<LoggregatorMessage> subscriber = new Subscriber<LoggregatorMessage>() {
            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                latch.countDown();
            }

            @Override
            public void onNext(LoggregatorMessage message) {
                long messageTimestamp = message.getTimestamp().getTime();
                if (newestTimestamp.get() < messageTimestamp) {
                    newestTimestamp.set(messageTimestamp);
                }
                count.incrementAndGet();
            }

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
        };
        Publisher<LoggregatorMessage> publisher = loggregatorClient.recent(RecentLogsRequest.builder()
                .applicationId(applicationId)
                .build());
        publisher.subscribe(subscriber);
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Subscriber timed out");
        } else if (error.get() != null) {
            throw error.get();
        } else {
            log.error("Last timestamp=%{} - got {} message", new Date(newestTimestamp.get()), count.get());
        }
    }

    @Test
    public void get_last_events() throws InterruptedException {
        TestSubscriber<ListEventsResponse> subscriber = new TestSubscriber<>();
        subscriber.assertThat(response -> {
            assertThat(response, is(notNullValue()));
            assertThat(response.getResources(), is(notNullValue()));
            response.getResources().stream()
                    .map(eventResource -> eventResource.getEntity().getType())
                    .forEach(log::debug);
        });

        Mono<ListEventsResponse> publisher = this.client
                .events().list(ListEventsRequest.builder().actee(applicationId).build());
        publisher.subscribe(subscriber);
        subscriber.verify(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void test_get_service_instance() throws InterruptedException {
        TestSubscriber<GetServiceInstanceResponse> subscriber = new TestSubscriber<>();
        subscriber.assertThat(response -> {
            assertThat(response, is(notNullValue()));
            assertThat(response.getMetadata().getId(), is(equalTo(serviceInstanceId)));
        });
        Mono<GetServiceInstanceResponse> publisher = this.client.serviceInstances()
                .get(GetServiceInstanceRequest.builder().serviceInstanceId(serviceInstanceId).build());
        publisher.subscribe(subscriber);
        subscriber.verify(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void test_bind_unbind_application() {
        Mono<CreateServiceBindingResponse> publisherBinding = client.serviceBindings().create
                (CreateServiceBindingRequest
                        .builder()
                        .applicationId(applicationId)
                        .serviceInstanceId(serviceInstanceId).build());
        CreateServiceBindingResponse responseBinding = publisherBinding.get();
        assertThat(responseBinding, is(notNullValue()));
        assertThat(responseBinding.getMetadata(), is(notNullValue()));
        assertThat(responseBinding.getEntity(), is(notNullValue()));
        assertThat(responseBinding.getEntity().getApplicationId(), is(equalTo(applicationId)));
        assertThat(responseBinding.getEntity().getServiceInstanceId(), is(equalTo(serviceInstanceId)));

        Mono<Void> response = client.serviceBindings().delete(DeleteServiceBindingRequest.builder().serviceBindingId
                (responseBinding.getMetadata().getId()).build());
        //will block until response ?
        response.get();

        //List to check that it works
        Mono<ListServiceBindingsResponse> publisherList = client.serviceBindings().list(ListServiceBindingsRequest
                .builder()
                .applicationId(applicationId)
                .serviceInstanceId(serviceInstanceId).build());
        ListServiceBindingsResponse listBinding = publisherList.get();
        assertThat(listBinding, is(notNullValue()));
        assertThat(listBinding.getResources(), is(notNullValue()));
        assertThat(listBinding.getResources().size(), is(equalTo(0)));

    }

    @PropertySource("classpath:test.properties")
    public static class SandboxConfiguration {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

}