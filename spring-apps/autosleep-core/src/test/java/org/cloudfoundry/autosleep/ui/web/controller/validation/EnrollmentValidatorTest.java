package org.cloudfoundry.autosleep.ui.web.controller.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.EnrollmentConfig;
import org.cloudfoundry.autosleep.ui.servicebroker.service.InvalidParameterException;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.ui.web.model.OrgEnrollmentConfigRequest;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

@RunWith(MockitoJUnitRunner.class)
public class EnrollmentValidatorTest {

    @Mock
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader;

    @Mock
    private ParameterReader<EnrollmentConfig.EnrollmentParameters.EnrollmentState> stateReader;

    @Mock
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader;

    @Mock
    private ParameterReader<Duration> idleDurationReader;

    @InjectMocks
    private EnrollmentValidator enrollmentValidator = new EnrollmentValidator();

    @Mock
    private OrgEnrollmentConfigRequest request;

    private Errors errors = null;

    @Before
    public void init() {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");
    }

    @Test
    public void test_supports_returns_true_for_correct_class() throws Exception {
        assertTrue(enrollmentValidator.supports(OrgEnrollmentConfigRequest.class));
        assertFalse(enrollmentValidator.supports(this.getClass()));
    }

    @Test
    public void test_supports_returns_false_for_any_other_class() throws Exception {
        assertFalse(enrollmentValidator.supports(this.getClass()));
    }

    @Test
    public void test_validate_idle_duration_to_throw_an_error_with_wrong_input() throws Exception {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");
        when(request.getIdleDuration()).thenReturn("PT_invalid_idle_duration");
        when(idleDurationReader.readParameter("PT_invalid_idle_duration", false)).thenThrow(
                new InvalidParameterException(Config.ServiceInstanceParameters.IDLE_DURATION,
                        "Error"));
        enrollmentValidator.validate(request, errors);
        assertTrue(errors.getErrorCount() == 1);
    }

    @Test
    public void test_validate_exclude_spaces_to_throw_an_error_with_wrong_input() throws Exception {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");
        when(request.getExcludeSpacesFromAutoEnrollment()).thenReturn("*_invalid_space_pattern");
        when(excludeFromAutoEnrollmentReader.readParameter("*_invalid_space_pattern", false))
                .thenThrow(new InvalidParameterException("exclude-spaces-from-auto-enrollment",
                        "Error"));
        enrollmentValidator.validate(request, errors);
        assertTrue(errors.getErrorCount() == 1);
    }

    @Test
    public void test_validate_auto_enrollment_to_throw_an_error_with_wrong_input()
            throws Exception {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");
        when(request.getAutoEnrollment()).thenReturn("not_standard");
        when(autoEnrollmentReader.readParameter("not_standard", false))
                .thenThrow(new InvalidParameterException("auto-enrollment", "Error"));
        enrollmentValidator.validate(request, errors);
        assertTrue(errors.getErrorCount() == 1);
    }

    @Test
    public void test_validate_enrollment_state_to_throw_an_error_with_wrong_input()
            throws Exception {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");
        when(request.getState()).thenReturn("not_backoffice_enrolled");
        when(stateReader.readParameter("not_backoffice_enrolled", false))
                .thenThrow(new InvalidParameterException("state", "Error"));
        enrollmentValidator.validate(request, errors);
        assertTrue(errors.getErrorCount() == 1);
    }

    @Test
    public void test_validate_all_properties_with_no_errors() throws Exception {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");
        when(request.getIdleDuration()).thenReturn("PT2M");
        when(request.getExcludeSpacesFromAutoEnrollment()).thenReturn(".*correct_space");
        when(request.getAutoEnrollment()).thenReturn("standard");
        when(request.getState()).thenReturn("backoffice_enrolled");
        enrollmentValidator.validate(request, errors);
        assertTrue(errors.getErrorCount() == 0);
    }

    @Test
    public void test_validate_all_properties_with_all_errors() throws Exception {
        errors = new MapBindingResult(new HashMap<>(), "OrgEnrollmentConfig");

        when(request.getIdleDuration()).thenReturn("PT_invalid_idle_duration");
        when(idleDurationReader.readParameter("PT_invalid_idle_duration", false)).thenThrow(
                new InvalidParameterException(Config.ServiceInstanceParameters.IDLE_DURATION,
                        "Error"));

        when(request.getExcludeSpacesFromAutoEnrollment()).thenReturn("*_invalid_space_pattern");
        when(excludeFromAutoEnrollmentReader.readParameter("*_invalid_space_pattern", false))
                .thenThrow(new InvalidParameterException("exclude-spaces-from-auto-enrollment",
                        "Error"));

        when(request.getAutoEnrollment()).thenReturn("non_standard");
        when(autoEnrollmentReader.readParameter("non_standard", false))
                .thenThrow(new InvalidParameterException("auto-enrollment", "Error"));

        when(request.getState()).thenReturn("not_backoffice_enrolled");
        when(stateReader.readParameter("not_backoffice_enrolled", false))
                .thenThrow(new InvalidParameterException("state", "Error"));

        enrollmentValidator.validate(request, errors);
        assertTrue(errors.getErrorCount() == 4);
    }

}
