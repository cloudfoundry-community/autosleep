package org.cloudfoundry.autosleep.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public enum LastDateComputer {
    INSTANCE;

    public Instant computeLastDate(Instant lastLog, Instant lastEvent) {
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
            log.debug("computeLastDate - lastEvent.isAfter(lastLog) = {}", lastEvent.isAfter(lastLog));
            return lastEvent.isAfter(lastLog) ? lastEvent : lastLog;
        }
    }
}
