package org.cloudfoundry.autosleep.remote;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Slf4j
public class ApplicationInfo {
    private final Instant lastEvent;

    private final Instant lastLog;

    private final CloudApplication.AppState state;

    /**
     * Return which ever date is the most recent (between last deploy event and last log).
     *
     * @return Most recent date
     */
    public Instant getLastActionDate() {
        if (lastLog == null) {
            if (lastEvent == null) {
                // from what we understood, events will always be returned, whereas recent logs may be empty.
                log.error("Last event is not supposed to be null");
                return null;
            }
            return lastEvent;
        } else if (lastEvent == null) {
            log.error("Last event is not supposed to be null");
            return lastLog;
        } else {
            log.debug("getLastActionDate - lastEvent.isAfter(lastLog) = {}", lastEvent.isAfter(lastLog));
            return lastEvent.isAfter(lastLog) ? lastEvent : lastLog;
        }
    }


}
