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
