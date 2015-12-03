package org.cloudfoundry.autosleep.config;

import java.time.Duration;

public interface Config {
    Duration defaultInactivityPeriod = Duration.ofDays(1);

    int nbThreadForTask = 5;

    Duration delayBeforeFirstServiceCheck = Duration.ofSeconds(10);

    interface Path {
        String dashboardPrefix = "/dashboard/";
    }


    interface EnvKey {
        String password = "security.user.password";

        String cfPassword = "cf.client.password";
        String cfUserName = "cf.client.username";
        String cfClientId = "cf.client.clientId";
        String cfClientSecret = "cf.client.clientSecret";
        String cfSkipSSLValidation = "cf.client.skip.ssl.validation";
        String cfEndPoint = "cf.client.target.endpoint";

        String cfEncodingSecret = "cf.security.password.encodingSecret";

    }
}
