package org.cloudfoundry.autosleep;

import java.time.Duration;

public interface Config {
    Duration defaultInactivityPeriod = Duration.ofDays(1);
}
