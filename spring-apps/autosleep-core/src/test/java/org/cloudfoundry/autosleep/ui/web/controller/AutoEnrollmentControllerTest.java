package org.cloudfoundry.autosleep.ui.web.controller;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.regex.Pattern;

import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.dao.model.OrgEnrollmentConfig;
import org.cloudfoundry.autosleep.access.dao.repositories.OrgEnrollmentConfigRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.EnrollmentConfig;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.ui.web.controller.validation.EnrollmentValidator;
import org.cloudfoundry.autosleep.ui.web.model.OrgEnrollmentConfigRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

@RunWith(MockitoJUnitRunner.class)
public class AutoEnrollmentControllerTest {

    @Mock
    private OrgEnrollmentConfigRepository orgEnrollmentConfigRepository;

    @InjectMocks
    private AutoEnrollmentController autoEnrollmentController;

    @Mock
    private BindingResult result;

    @Mock
    private CloudFoundryApiService cloudFoundryApi;

    @Mock
    private EnrollmentValidator validator;

    @Mock
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader;

    @Mock
    private ParameterReader<EnrollmentConfig.EnrollmentParameters.EnrollmentState> stateReader;

    @Mock
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader;

    @Mock
    private ParameterReader<Duration> idleDurationReader;

    @Test
    public void test_getEnrolledOrganization_ok() throws Exception {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfig orgConfig = OrgEnrollmentConfig.builder().organizationGuid(fakeOrgGuid)
                .build();
        when(orgEnrollmentConfigRepository.findOne(fakeOrgGuid)).thenReturn(orgConfig);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .getEnrolledOrganization(fakeOrgGuid);

        assertTrue(response.getStatusCode() == HttpStatus.OK);

    }

    @Test
    public void test_getEnrolledOrganization_not_found() throws Exception {
        String fakeOrgGuid = "incorrect-fake-organization-guid";
        when(orgEnrollmentConfigRepository.findOne(fakeOrgGuid)).thenReturn(null);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .getEnrolledOrganization(fakeOrgGuid);

        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void test_deleteOrganization_ok() throws CloudFoundryException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfig orgConfig = OrgEnrollmentConfig.builder().organizationGuid(fakeOrgGuid)
                .build();
        when(orgEnrollmentConfigRepository.findOne(fakeOrgGuid)).thenReturn(orgConfig);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .deleteOrganization(fakeOrgGuid);

        assertTrue(response.getStatusCode() == HttpStatus.OK);
    }

    @Test
    public void test_deleteOrganization_not_found() throws CloudFoundryException {
        String fakeOrgGuid = "incorrect-fake-organization-guid";
        when(orgEnrollmentConfigRepository.findOne(fakeOrgGuid)).thenReturn(null);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .deleteOrganization(fakeOrgGuid);

        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void test_enrolOrganization_created_ok()
            throws CloudFoundryException, BindException, MalformedURLException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder()
                .organizationGuid(fakeOrgGuid).idleDuration("PT2M").autoEnrollment("standard")
                .excludeSpacesFromAutoEnrollment(".*space").state("backoffice_enrolled").build();
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .enrolOrganization(fakeOrgGuid, request, result);
        assertTrue(response.getStatusCode() == HttpStatus.CREATED);
        assertTrue(response.getHeaders().getFirst("Location")
                .equals("/v1/enrolled-orgs/" + fakeOrgGuid));
    }

    @Test
    public void test_enrolOrganization_updated_ok() throws CloudFoundryException, BindException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder()
                .organizationGuid(fakeOrgGuid).idleDuration("PT2M").autoEnrollment("standard")
                .excludeSpacesFromAutoEnrollment(".*space").state("backoffice_enrolled").build();
        when(orgEnrollmentConfigRepository.exists(fakeOrgGuid)).thenReturn(true);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .enrolOrganization(fakeOrgGuid, request, result);
        assertTrue(response.getStatusCode() == HttpStatus.OK);
    }

    @Test
    public void test_enrolOrganization_with_bind_exception()
            throws CloudFoundryException, BindException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder()
                .organizationGuid(fakeOrgGuid).build();
        when(result.hasErrors()).thenReturn(true);
        verifyThrown(() -> autoEnrollmentController.enrolOrganization(fakeOrgGuid, request, result),
                BindException.class);

    }
}
