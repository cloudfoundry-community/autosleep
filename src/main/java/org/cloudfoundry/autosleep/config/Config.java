package org.cloudfoundry.autosleep.config;

import java.time.Duration;

public interface Config {

    int CF_API_TIMEOUT_IN_S = 8;

    Duration DEFAULT_INACTIVITY_PERIOD = Duration.ofDays(1);

    Duration DELAY_BEFORE_FIRST_SERVICE_CHECK = Duration.ofSeconds(10);

    int NB_THREAD_FOR_TASK = 5;

    interface CloudFoundryAppState {

        String STARTED = "STARTED";

        String STOPPED = "STOPPED";

    }

    interface DefaultClientIdentification {

        String ID = "cf";

        String SECRET = "";

    }

    interface EnvKey {

        String APPLICATION_DESCRIPTION_ENVIRONMENT_KEY = "VCAP_APPLICATION";

        String CF_CLIENT_ID = "cf.client.clientId";

        String CF_CLIENT_SECRET = "cf.client.clientSecret";

        String CF_ENCODING_SECRET = "cf.security.password.encodingSecret";

        String CF_ENDPOINT = "cf.client.target.endpoint";

        String CF_PASSWORD = "cf.client.password";

        String CF_SERVICE_BROKER_ID = "cf.service.broker.id";

        String CF_SERVICE_PLAN_ID = "cf.service.plan.id";

        String CF_SKIP_SSL_VALIDATION = "cf.client.skip.ssl.validation";

        String CF_USERNAME = "cf.client.username";

        String SECURITY_PASSWORD = "security.user.password";

    }

    interface Path {

        String API_CONTEXT = "/api";

        String APPLICATIONS_SUB_PATH = "/applications/";

        String DASHBOARD_CONTEXT = "/dashboard";

        String PROXY_CONTEXT = "/proxy";

        String SERVICES_SUB_PATH = "/services/";

        String SERVICE_BROKER_SERVICE_CONTROLLER_BASE_PATH = "/v2/service_instances";

    }

    interface RouteBindingParameters {

        String linkedApplicationBindingId = "application-binding-id";

        String linkedApplicationId = "application-id";
    }

    interface ServiceCatalog {

        String DEFAULT_SERVICE_BROKER_ID = "autosleep";

        String DEFAULT_SERVICE_PLAN_ID = "default";
    }

    interface ServiceInstanceParameters {

        enum Enrollment {
            standard, forced
        }

        String AUTO_ENROLLMENT = "auto-enrollment";

        String EXCLUDE_FROM_AUTO_ENROLLMENT = "exclude-from-auto-enrollment";

        String IDLE_DURATION = "idle-duration";

        String SECRET = "secret";

    }
}
