package org.cloudfoundry.autosleep.remote;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.LocalDateTime;

@Data
@Slf4j
public class ApplicationInfo {
    private final LocalDateTime lastEvent;
    private final LocalDateTime lastLog;
    private final CloudApplication.AppState state;

    /** Return which ever date is the most recent (between last deploy event and last log).
     *
     * @return Most recent date
     */
    public LocalDateTime getLastActionDate() {

        if (lastLog == null) {
            if (lastEvent == null) {
                // from what we understood, events will always be returned, whereas recent logs may be empty.
                log.error("Last event is not supposed to be null");
                return null;
            }
            return lastEvent;
        }

        return lastEvent.isAfter(lastLog) ? lastEvent : lastLog;
    }



}
