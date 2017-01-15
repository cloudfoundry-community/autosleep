package org.cloudfoundry.autosleep.access.dao.model;

import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.regex.Pattern;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.EnrollmentConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrgEnrollmentConfigTest {

    private static final String FAKE_ORG_ID = "fakeOrgId";
    private static final Duration DURATION = Duration.parse("PT2M");
    private static final Pattern PATTERN = Pattern.compile(".*space");

    @Test
    public void test_enrolOrg_without_parameters() {

        OrgEnrollmentConfig enrolledOrg = OrgEnrollmentConfig.builder()
                .organizationGuid(FAKE_ORG_ID).build();
        assertTrue(enrolledOrg.getOrganizationGuid().equalsIgnoreCase(FAKE_ORG_ID));
        // To be enhanced later when defaulting is taken care by the poller
    }

    @Test
    public void test_enrolOrg_with_parameters_for_org() {
        OrgEnrollmentConfig enrolledOrg = OrgEnrollmentConfig.builder()
                .organizationGuid(FAKE_ORG_ID).idleDuration(DURATION)
                .excludeSpacesFromAutoEnrollment(PATTERN)
                .autoEnrollment(Config.ServiceInstanceParameters.Enrollment.standard)
                .state(EnrollmentConfig.EnrollmentParameters.EnrollmentState.enrolled).build();
        assertTrue(enrolledOrg != null);
    }

    @Test
    public void test_enrolOrg_and_switch_state_to_recursive_opted_out() {
        OrgEnrollmentConfig enrolledOrg = OrgEnrollmentConfig.builder()
                .organizationGuid(FAKE_ORG_ID).idleDuration(DURATION)
                .excludeSpacesFromAutoEnrollment(PATTERN)
                .autoEnrollment(Config.ServiceInstanceParameters.Enrollment.standard)
                .state(EnrollmentConfig.EnrollmentParameters.EnrollmentState.enrolled).build();
        assertTrue(enrolledOrg
                .getState() == EnrollmentConfig.EnrollmentParameters.EnrollmentState.enrolled);
        enrolledOrg.setState(
                EnrollmentConfig.EnrollmentParameters.EnrollmentState.backoffice_recursive_opted_out);
        assertTrue(enrolledOrg
                .getState() == EnrollmentConfig.EnrollmentParameters.EnrollmentState.backoffice_recursive_opted_out);
    }

    @Test
    public void test_enrolOrg_and_later_opt_out_or_delete() {
        OrgEnrollmentConfig enrolledOrg = OrgEnrollmentConfig.builder()
                .organizationGuid(FAKE_ORG_ID).idleDuration(DURATION)
                .excludeSpacesFromAutoEnrollment(PATTERN)
                .autoEnrollment(Config.ServiceInstanceParameters.Enrollment.standard)
                .state(EnrollmentConfig.EnrollmentParameters.EnrollmentState.enrolled).build();
        assertTrue(enrolledOrg
                .getState() == EnrollmentConfig.EnrollmentParameters.EnrollmentState.enrolled);
        enrolledOrg.setState(
                EnrollmentConfig.EnrollmentParameters.EnrollmentState.backoffice_opted_out);
        assertTrue(enrolledOrg
                .getState() == EnrollmentConfig.EnrollmentParameters.EnrollmentState.backoffice_opted_out);

    }

}
