package org.cloudfoundry.autosleep.ui.web.controller;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.validation.ObjectError;

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
        verify(orgEnrollmentConfigRepository, times(1)).delete(fakeOrgGuid);
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
    public void test_enrolOrganization_created_with_null_parameters_ok()
            throws CloudFoundryException, BindException, MalformedURLException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder().build();

        when(cloudFoundryApi.isValidOrganization(fakeOrgGuid)).thenReturn(true);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .enrolOrganization(fakeOrgGuid, request, result);

        verify(orgEnrollmentConfigRepository, times(1)).exists(fakeOrgGuid);
        verify(orgEnrollmentConfigRepository, times(1)).save(any(OrgEnrollmentConfig.class));

        assertTrue(response.getStatusCode() == HttpStatus.CREATED);
        assertTrue(response.getHeaders().getFirst("Location")
                .equals(EnrollmentConfig.Path.ORG_AUTO_ENROLMENT_BASE_PATH + fakeOrgGuid));
    }

    @Test
    public void test_enrolOrganization_created_ok()
            throws CloudFoundryException, BindException, MalformedURLException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder()
                .idleDuration("PT2M").autoEnrollment("standard")
                .excludeSpacesFromAutoEnrollment(".*space").state("backoffice_enrolled").build();
        when(cloudFoundryApi.isValidOrganization(fakeOrgGuid)).thenReturn(true);

        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .enrolOrganization(fakeOrgGuid, request, result);

        verify(orgEnrollmentConfigRepository, times(1)).exists(fakeOrgGuid);
        verify(orgEnrollmentConfigRepository, times(1)).save(any(OrgEnrollmentConfig.class));

        assertTrue(response.getStatusCode() == HttpStatus.CREATED);
        assertTrue(response.getHeaders().getFirst("Location")
                .equals(EnrollmentConfig.Path.ORG_AUTO_ENROLMENT_BASE_PATH + fakeOrgGuid));
    }

    @Test
    public void test_enrolOrganization_updated_ok() throws CloudFoundryException, BindException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder()
                .idleDuration("PT2M").autoEnrollment("standard")
                .excludeSpacesFromAutoEnrollment(".*space").state("backoffice_enrolled").build();
        when(cloudFoundryApi.isValidOrganization(fakeOrgGuid)).thenReturn(true);
        when(orgEnrollmentConfigRepository.exists(fakeOrgGuid)).thenReturn(true);
        ResponseEntity<OrgEnrollmentConfig> response = autoEnrollmentController
                .enrolOrganization(fakeOrgGuid, request, result);

        verify(orgEnrollmentConfigRepository, times(1)).exists(fakeOrgGuid);
        verify(orgEnrollmentConfigRepository, times(1)).save(any(OrgEnrollmentConfig.class));
        assertTrue(response.getStatusCode() == HttpStatus.OK);
    }

    @Test
    public void test_enrolOrganization_throws_bind_exception()
            throws CloudFoundryException, BindException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder().build();
        when(cloudFoundryApi.isValidOrganization(fakeOrgGuid)).thenReturn(true);
        when(result.hasErrors()).thenReturn(true);
        verifyThrown(() -> autoEnrollmentController.enrolOrganization(fakeOrgGuid, request, result),
                BindException.class);
    }

    @Test
    public void test_enrolOrganization_returns_not_found_for_invalid_org()
            throws CloudFoundryException, BindException {
        String fakeOrgGuid = "fake-organization-guid";
        OrgEnrollmentConfigRequest request = OrgEnrollmentConfigRequest.builder().build();
        when(cloudFoundryApi.isValidOrganization(fakeOrgGuid)).thenReturn(false);
        ResponseEntity<?> response = autoEnrollmentController.enrolOrganization(fakeOrgGuid,
                request, result);
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void test_handleBindException_returns_errors() throws Exception {
        BindException be = mock(BindException.class);
        ObjectError fakeError1 = new ObjectError("orgEnrollmentConfig", "fakeErrorMessage1");
        ObjectError fakeError2 = new ObjectError("orgEnrollmentConfig", "fakeErrorMessage2");
        when(be.getAllErrors()).thenReturn(Arrays.asList(fakeError1, fakeError2));

        HttpServletResponse response = mock(HttpServletResponse.class);
        ResponseEntity<List<ObjectError>> errors = autoEnrollmentController.handleBindException(be,
                response);
        assertTrue(errors.getBody() != null && errors.getBody().size() == 2);
        assertTrue(errors.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Test
    public void test_handleCloudFoundryException_returns_not_found() throws Exception {
        CloudFoundryException cfe = mock(CloudFoundryException.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ResponseEntity<Object> errorResponseEntity = autoEnrollmentController
                .handleCloudFoundryException(cfe, response);
        assertTrue(errorResponseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);

    }
}
