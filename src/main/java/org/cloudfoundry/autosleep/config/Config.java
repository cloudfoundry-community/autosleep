package org.cloudfoundry.autosleep.config;

import java.time.Duration;

public interface Config {
    Duration defaultInactivityPeriod = Duration.ofDays(1);

    int nbThreadForTask = 5;

    Duration delayBeforeFirstServiceCheck = Duration.ofSeconds(10);

    interface Path {
        String dashboardPrefix = "/dashboard/";
    }
}
