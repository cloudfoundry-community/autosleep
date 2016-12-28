package org.cloudfoundry.autosleep.config;

public interface EnrollmentConfig {
    interface EnrollmentParameters {
        enum EnrollmentState {
            // All spaces and apps inherit the configuration of the org or space unless
            // individually overridden either via SpaceEnrollmentConfig or specific service instance
            backoffice_enrolled,
            // All spaces and apps within the org or space are excluded from autosleep even if explicitly
            // enrolled via SpaceEnrollmentConfig or specific service instance
            backoffice_opted_out
        }
        
        String STATE = "state";
    }
}
