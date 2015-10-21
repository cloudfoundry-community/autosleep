package org.cloudfoundry.autosleep.remote;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationInfo {
    private final LocalDateTime lastDeployed;
    private final LocalDateTime lastLog;

    public LocalDateTime getLastEventTime() {
        return lastDeployed.isAfter(lastLog) ? lastDeployed : lastLog;
    }
}
