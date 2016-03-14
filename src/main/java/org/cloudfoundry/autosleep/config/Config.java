/**
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.config;

import java.time.Duration;

public interface Config {

    int CF_API_TIMEOUT_IN_S = 8;

    Duration DEFAULT_INACTIVITY_PERIOD = Duration.ofDays(1);

    Duration DELAY_BEFORE_FIRST_SERVICE_CHECK = Duration.ofSeconds(10);

    int NB_THREAD_FOR_TASK = 5;

    Duration PERIOD_BETWEEN_STATE_CHECKS_DURING_RESTART = Duration.ofSeconds(3);

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
