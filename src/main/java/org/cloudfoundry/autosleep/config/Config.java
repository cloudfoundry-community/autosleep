package org.cloudfoundry.autosleep.config;

import java.time.Duration;

public interface Config {
    Duration DEFAULT_INACTIVITY_PERIOD = Duration.ofDays(1);

    int NB_THREAD_FOR_TASK = 5;

    Duration DELAY_BEFORE_FIRST_SERVICE_CHECK = Duration.ofSeconds(10);

    interface Path {
        String API_CONTEXT = "/api";
        String SERVICES_SUB_PATH = "/services/";
        String APPLICATIONS_SUB_PATH = "/applications/";
        String DASHBOARD_CONTEXT = "/dashboard";
    }


    interface EnvKey {
        String SECURITY_PASSWORD = "security.user.password";

        String CF_PASSWORD = "cf.client.password";
        String CF_USERNAME = "cf.client.username";
        String CF_CLIENT_ID = "cf.client.clientId";
        String CF_CLIENT_SECRET = "cf.client.clientSecret";
        String CF_SKIP_SSL_VALIDATION = "cf.client.skip.ssl.validation";
        String CF_ENDPOINT = "cf.client.target.endpoint";

        String CF_ENCODING_SECRET = "cf.security.password.encodingSecret";

        String CF_SERVICE_BROKER_ID = "cf.service.broker.id";


        String APPLICATION_DESCRIPTION_ENVIRONMENT_KEY = "VCAP_APPLICATION";

    }

    interface ServiceInstanceParameters {

        String IDLE_DURATION = "idle-duration";

        String EXCLUDE_FROM_AUTO_ENROLLMENT = "exclude-from-auto-enrollment";

        String AUTO_ENROLLMENT = "auto-enrollment";

        String SECRET = "secret";

        enum Enrollment {
            standard , forced
        }
    }
}
