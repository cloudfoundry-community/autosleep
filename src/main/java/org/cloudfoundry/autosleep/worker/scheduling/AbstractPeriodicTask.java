/*
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

package org.cloudfoundry.autosleep.worker.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPeriodicTask implements Runnable {

    private final Clock clock;

    @Getter(value = AccessLevel.PROTECTED)
    private final Duration period;

    protected abstract String getTaskId();

    public Instant reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(getTaskId(), delta, this);
        return Instant.now().plus(delta);
    }

    public Instant rescheduleWithDefaultPeriod() {
        return reschedule(period);
    }

    public void start(Duration delay) {
        log.debug("start - {}", delay);
        clock.scheduleTask(getTaskId(), delay == null ? Duration.ofSeconds(0) : delay, this);
    }

    public void startNow() {
        start(Duration.ofSeconds(0));
    }

    public void stopTask() {
        clock.removeTask(getTaskId());
    }

}
