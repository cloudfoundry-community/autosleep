package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class AutosleepServiceInstanceServiceTest {

    private static final UUID ORG_TEST = UUID.randomUUID();

    private static final UUID SPACE_TEST = UUID.randomUUID();

    private static final UUID[] APP_TEST = {
            UUID.randomUUID(), UUID.randomUUID()
    };

    private static final String SERVICE_DEFINITION_ID = "serviceDefinitionId";

    private static final String PLAN_ID = "planId";

    private static final String SERVICE_INSTANCE_ID = "serviceInstanceId";


    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private GlobalWatcher globalWatcher;

    @InjectMocks
    private AutoSleepServiceInstanceService instanceService;

    private CreateServiceInstanceRequest createRequest;

    private UpdateServiceInstanceRequest updateRequest;

    private DeleteServiceInstanceRequest deleteRequest;

    private List<ApplicationIdentity> applications = Arrays.asList(APP_TEST).stream()
            .map(applicationUuid -> new ApplicationIdentity(applicationUuid, applicationUuid.toString()))
            .collect(Collectors.toList());

    @Before
    public void initService() {
        instanceService = new AutoSleepServiceInstanceService(serviceRepository, globalWatcher);


        createRequest = new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID, PLAN_ID,
                ORG_TEST.toString(), SPACE_TEST.toString());
        createRequest.withServiceInstanceId(SERVICE_INSTANCE_ID);

        updateRequest = new UpdateServiceInstanceRequest(SERVICE_DEFINITION_ID);
        updateRequest.withInstanceId(SERVICE_INSTANCE_ID);

        deleteRequest = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID, SERVICE_DEFINITION_ID, PLAN_ID);

    }

    @Test
    public void testCreateServiceInstance() throws Exception {
        //test null request
        try {
            instanceService.createServiceInstance(null);
            log.debug("Service instance created");
            fail("Succeed in creating instanceService with no request");
        } catch (NullPointerException s) {
            log.debug("{} occurred as expected", s.getClass().getSimpleName());
        }
        //test existing instanceService request
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));
        try {
            instanceService.createServiceInstance(createRequest);
            fail("Succeed in creating an already existing instanceService");
        } catch (ServiceInstanceExistsException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        //Should succeed
        createRequest.setParameters(Collections.singletonMap(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT10H"));
        ServiceInstance si = instanceService.createServiceInstance(createRequest);

        assertThat("Succeed in creating instanceService with inactivity parameter", si, is(notNullValue()));

        verify(globalWatcher, times(1)).watchServiceBindings(SERVICE_INSTANCE_ID, Config.delayBeforeFirstServiceCheck);
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));
    }

    @Test
    public void testGetServiceInstance() throws Exception {
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));

        org.cloudfoundry.community.servicebroker.model.ServiceInstance retrievedInstance = instanceService
                .getServiceInstance(
                        SERVICE_INSTANCE_ID);
        assertThat(retrievedInstance, is(notNullValue()));
        assertThat(retrievedInstance.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));
    }

    @Test
    public void testUpdateServiceInstance() throws Exception {

        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(null);
        try {
            instanceService.updateServiceInstance(updateRequest);
            fail("update not supposed to work on an unknown instanceService id");
        } catch (ServiceInstanceDoesNotExistException e) {
            log.debug("{} occurred as expected", e.getClass().getSimpleName());
        }
        when(serviceRepository.findOne(SERVICE_INSTANCE_ID)).thenReturn(new AutosleepServiceInstance(createRequest));
        instanceService.updateServiceInstance(updateRequest);
        verify(serviceRepository, times(1)).save(any(AutosleepServiceInstance.class));

    }

    @Test
    public void testDeleteServiceInstance() throws Exception {
        ServiceInstance si = instanceService.deleteServiceInstance(deleteRequest);
        verify(serviceRepository, times(1)).delete(SERVICE_INSTANCE_ID);
        assertThat(si, is(notNullValue()));
        assertThat(si.getServiceInstanceId(), is(equalTo(SERVICE_INSTANCE_ID)));

    }

}