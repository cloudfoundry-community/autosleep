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
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesResponse;
import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationResponse;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingResponse;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingResponse;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsResponse;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceToRouteRequest;
import org.cloudfoundry.client.v2.serviceinstances.BindServiceInstanceToRouteResponse;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.util.test.TestSubscriber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestIntegration.SandboxConfiguration.class)
public class TestIntegration {

	private static final long DEFAULT_TIMEOUT_IN_SECONDS = 2;

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

	@Value("${test.route.id}")
	private String routeId;

	private CloudFoundryClient client;

	private DopplerClient loggregatorClient;

	@Before
	public void buildClient() {
		if (client == null) {
			log.debug("Building to {}", cfUrl);
			ConnectionContext connectionContext = DefaultConnectionContext.builder()
					.apiHost(cfUrl)
					.skipSslValidation(skipVerification)
					.build();
			TokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
					.password(cfPassword)
					.username(cfUsername)
					.clientId(cfClientId)
					.clientSecret(cfClientSecret)
					.build();

			this.client = ReactorCloudFoundryClient.builder()
					.connectionContext(connectionContext)
					.tokenProvider(tokenProvider)
					.build();

			this.loggregatorClient = ReactorDopplerClient.builder()
					.connectionContext(connectionContext)
					.tokenProvider(tokenProvider)
					.build();
		}
	}

	@Test
	public void get_application_poll() {
		log.debug("get_application_get");
		Mono<GetApplicationResponse> publisher = this.client
				.applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationId).build());
		//wait for 30s by default
		GetApplicationResponse response = publisher.block();
		assertThat(response, is(notNullValue()));
		assertThat(response.getMetadata(), is(notNullValue()));
		assertThat(response.getEntity(), is(notNullValue()));
		assertThat(response.getMetadata().getId(), is(equalTo(applicationId)));
		assertThat(response.getEntity().getState(), is(notNullValue()));

		log.debug("get_application_get - {} found (state = {})", response.getEntity().getName(), response.getEntity().getState());

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
		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));

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

		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));

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
			log.debug("test_start - end");
		});
		Mono<UpdateApplicationResponse> publisherStart = client.applicationsV2().update
				(UpdateApplicationRequest.builder().applicationId(applicationId).state("STARTED").build());
		publisherStart.subscribe(subscriber);
		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));
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
		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));

	}

	@Test
	public void get_last_logs() throws Throwable {

		final AtomicInteger count = new AtomicInteger();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicLong newestTimestamp = new AtomicLong();
		Flux<LogMessage> publisher = loggregatorClient.recentLogs(RecentLogsRequest.builder()
				.applicationId(applicationId)
				.build())
				.filter(envelope -> envelope.getLogMessage() != null)
				.map(Envelope::getLogMessage);

		publisher.subscribe(message -> {
					long messageTimestamp = message.getTimestamp();
					if (newestTimestamp.get() < messageTimestamp) {
						newestTimestamp.set(messageTimestamp);
					}
					count.incrementAndGet();
					log.debug("{} - {} - {} - {}",
							message.getSourceType(),
							message.getSourceInstance(),
							message.getMessageType(),
							message.getMessage());
				},
				throwable -> {
					log.error("Error got", throwable);
					latch.countDown();
				},
				latch::countDown);

		if (!latch.await(10, TimeUnit.SECONDS)) {
			throw new IllegalStateException("Subscriber timed out");
		} else {
			log.error("end - newestTimestamp={} - count={}", newestTimestamp.get(), count.get());
		}
	}

	@Test
	public void get_instances() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Mono<ApplicationInstancesResponse> publisher = this.client.applicationsV2()
				.instances(ApplicationInstancesRequest.builder()
						.applicationId(applicationId)
						.build());
		publisher.subscribe(response -> {
					response.getInstances().entrySet().forEach(entry -> {
						log.debug("{} - {}", entry.getKey(), entry.getValue().getState());
					});
				},
				throwable -> {
					log.error("error", throwable);
					latch.countDown();
				},
				latch::countDown);
		if (!latch.await(10, TimeUnit.SECONDS)) {
			throw new IllegalStateException("Subscriber timed out");
		} else {
			log.error("end - get_instances");
		}
	}

	@Test
	public void get_last_events() throws InterruptedException {
		TestSubscriber<ListEventsResponse> subscriber = new TestSubscriber<>();
		subscriber.assertThat(response -> {
			assertThat(response, is(notNullValue()));
			assertThat(response.getResources(), is(notNullValue()));
			response.getResources().stream()
					.map(eventResource -> eventResource.getEntity().getTimestamp())
					.forEach(log::debug);
			if (response.getResources().size() > 0) {
				Instant time = Instant.parse(response.getResources().get(response.getResources().size() - 1).getEntity().getTimestamp());
				log.debug("This is my timestamp {}", time);
			}
		});

		Mono<ListEventsResponse> publisher = this.client
				.events().list(ListEventsRequest.builder().actee(applicationId).build());
		publisher.subscribe(subscriber);
		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));
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
		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));
	}

	@Test
	public void test_bind_unbind_application() {
		Mono<CreateServiceBindingResponse> publisherBinding = client.serviceBindingsV2().create
				(CreateServiceBindingRequest
						.builder()
						.applicationId(applicationId)
						.serviceInstanceId(serviceInstanceId).build());
		CreateServiceBindingResponse responseBinding = publisherBinding.block();
		assertThat(responseBinding, is(notNullValue()));
		assertThat(responseBinding.getMetadata(), is(notNullValue()));
		assertThat(responseBinding.getEntity(), is(notNullValue()));
		assertThat(responseBinding.getEntity().getApplicationId(), is(equalTo(applicationId)));
		assertThat(responseBinding.getEntity().getServiceInstanceId(), is(equalTo(serviceInstanceId)));

		Mono<DeleteServiceBindingResponse> response = client.serviceBindingsV2().delete(DeleteServiceBindingRequest.builder()
				.serviceBindingId(responseBinding.getMetadata().getId()).build());
		//will block until response ?
		response.block();

		//List to check that it works
		Mono<ListServiceBindingsResponse> publisherList = client.serviceBindingsV2().list(ListServiceBindingsRequest
				.builder()
				.applicationId(applicationId)
				.serviceInstanceId(serviceInstanceId).build());
		ListServiceBindingsResponse listBinding = publisherList.block();
		assertThat(listBinding, is(notNullValue()));
		assertThat(listBinding.getResources(), is(notNullValue()));
		assertThat(listBinding.getResources().size(), is(equalTo(0)));

	}

	@Test
	public void test_get_app_routes() throws InterruptedException {

		TestSubscriber<ListApplicationRoutesResponse> subscriber = new TestSubscriber<>();
		subscriber.assertThat(response -> {
			assertThat(response, is(notNullValue()));
			assertThat(response.getResources(), is(notNullValue()));
			response.getResources().stream()
					.map(routeResource -> routeResource.getMetadata().getUrl())
					.forEach(log::debug);
		});

		Mono<ListApplicationRoutesResponse> publisher = this.client.applicationsV2().listRoutes(
				ListApplicationRoutesRequest.builder().applicationId(applicationId).build());
		publisher.subscribe(subscriber);
		subscriber.verify(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS));
	}

	@Test
	public void test_bind_unbind_route() {
		Mono<BindServiceInstanceToRouteResponse> publisherBinding = client.serviceInstances().bindToRoute(
				BindServiceInstanceToRouteRequest
						.builder()
						.serviceInstanceId(serviceInstanceId)
						.routeId(routeId).build());
		BindServiceInstanceToRouteResponse responseBinding = publisherBinding.block();
		assertThat(responseBinding, is(notNullValue()));
		assertThat(responseBinding.getMetadata(), is(notNullValue()));
		assertThat(responseBinding.getMetadata().getId(), is(notNullValue()));
		assertThat(responseBinding.getEntity(), is(notNullValue()));

		Mono<DeleteServiceBindingResponse> response = client.serviceBindingsV2().delete(DeleteServiceBindingRequest.builder()
				.serviceBindingId(responseBinding.getMetadata().getId())
				.build());
		//will block until response ?
		response.block();

		//List to check that it works
		Mono<ListServiceBindingsResponse> publisherList = client.serviceBindingsV2().list(ListServiceBindingsRequest
				.builder()
				.applicationId(applicationId)
				.serviceInstanceId(serviceInstanceId).build());
		ListServiceBindingsResponse listBinding = publisherList.block();
		assertThat(listBinding, is(notNullValue()));
		assertThat(listBinding.getResources(), is(notNullValue()));
		assertThat(listBinding.getResources().size(), is(equalTo(0)));
	}

}