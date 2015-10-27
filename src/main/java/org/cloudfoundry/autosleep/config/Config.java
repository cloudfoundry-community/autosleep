package org.cloudfoundry.autosleep.config;

import java.time.Duration;

public interface Config {
    Duration defaultInactivityPeriod = Duration.ofDays(1);
}
