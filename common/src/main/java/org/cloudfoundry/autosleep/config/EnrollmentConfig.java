package org.cloudfoundry.autosleep.config;

public interface EnrollmentConfig {
    interface EnrollmentParameters {
        enum EnrollmentState {
            // All spaces and apps inherit the configuration of the org or space
            // unless
            // individually overridden either via SpaceEnrollmentConfig or
            // specific service instance
            enrolled,
            // All spaces and apps within the org or space are excluded from
            // autosleep even if explicitly
            // enrolled via SpaceEnrollmentConfig or specific service instance
            backoffice_recursive_opted_out,
            // All explicitly created space level service instances continue to
            // function according
            // to their own configuration. The organization will not be auto
            // enrolled
            backoffice_opted_out,
        }

        String STATE = "state";
    }

    interface Path {
        String ORG_AUTO_ENROLMENT_BASE_PATH = "/v1/enrolled-orgs/";
    }
}
