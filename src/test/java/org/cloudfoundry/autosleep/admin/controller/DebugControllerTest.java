package org.cloudfoundry.autosleep.admin.controller;

import org.cloudfoundry.autosleep.AbstractRestTest;
import org.cloudfoundry.autosleep.admin.model.ServiceBinding;
import org.cloudfoundry.autosleep.admin.model.ServiceInstance;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceInstance;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@ActiveProfiles("in-memory")
public class DebugControllerTest extends AbstractRestTest {

    private static final String serviceInstanceId = "serviceInstanceId";

    private static final String serviceBindingId = "serviceBindingId";

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Before
    public void init() {
        serviceRepository.deleteAll();
        CreateServiceInstanceRequest createRequestTemplate = new CreateServiceInstanceRequest("definition", "plan",
                "org",
                "space");
        serviceRepository.save(
                new AutoSleepServiceInstance(createRequestTemplate.withServiceInstanceId(serviceInstanceId)));
        bindingRepository.save(new AutoSleepServiceBinding(serviceBindingId, serviceInstanceId, null, null, "app"));
    }

    @Test
    public void testListServiceInstances() {
        ResponseEntity<ServiceInstance[]> response = prepare("/admin/debug/services/servicesinstances/",
                HttpMethod.GET, null, ServiceInstance[].class).withBasicAuthentication(username, password).call();
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(response.getBody().length, is(equalTo(1)));
        assertThat(response.getBody()[0].getInstanceId(), is(equalTo(serviceInstanceId)));
    }

    @Test
    public void testListServiceBindings() {
        ResponseEntity<ServiceBinding[]> response = prepare(
                "/admin/debug/services/servicebindings/" + serviceInstanceId,
                HttpMethod.GET, null, ServiceBinding[].class).withBasicAuthentication(username, password).call();
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(response.getBody().length, is(equalTo(1)));
        assertThat(response.getBody()[0].getId(), is(equalTo(serviceBindingId)));
    }
}