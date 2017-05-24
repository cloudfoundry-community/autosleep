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

package org.cloudfoundry.autosleep.access.cloudfoundry.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.cloudfoundry.autosleep.access.dao.model.ApplicationInfo;

@Getter
@ToString
public class ApplicationActivity {

    private final ApplicationIdentity application;

    private final ApplicationInfo.DiagnosticInfo.ApplicationEvent lastEvent;

    private final ApplicationInfo.DiagnosticInfo.ApplicationLog lastLog;

    private final String state;


    @Builder
    ApplicationActivity(ApplicationIdentity application,
                        ApplicationInfo.DiagnosticInfo.ApplicationEvent lastEvent,
                        ApplicationInfo.DiagnosticInfo.ApplicationLog lastLog,
                        String state) {
        this.application = application;
        this.lastEvent = lastEvent;
        this.lastLog = lastLog;
        this.state = state;
    }

}
