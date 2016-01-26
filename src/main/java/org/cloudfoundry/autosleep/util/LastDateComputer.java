package org.cloudfoundry.autosleep.util;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;

import java.time.Instant;

@Slf4j
public class LastDateComputer {

    public static Instant computeLastDate(ApplicationInfo.DiagnosticInfo.ApplicationLog lastLog,
                                          ApplicationInfo.DiagnosticInfo.ApplicationEvent lastEvent) {
        if (lastLog == null) {
            if (lastEvent == null) {
                // from what we understood, events will always be returned, whereas recent logs may be empty.
                log.error("Last event is not supposed to be null");
                return null;
            }
            return lastEvent.getTimestamp();
        } else if (lastEvent == null) {
            log.error("Last event is not supposed to be null");
            return lastLog.getTimestamp();
        } else {
            log.debug("computeLastDate - lastEvent.isAfter(lastLog) = {}", lastEvent.getTimestamp()
                    .isAfter(lastLog.getTimestamp()));
            return lastEvent.getTimestamp().isAfter(lastLog.getTimestamp())
                    ? lastEvent.getTimestamp() : lastLog.getTimestamp();
        }
    }
}
