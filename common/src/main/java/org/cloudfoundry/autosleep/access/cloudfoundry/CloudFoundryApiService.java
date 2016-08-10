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

package org.cloudfoundry.autosleep.access.cloudfoundry;

import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationActivity;
import org.cloudfoundry.autosleep.access.cloudfoundry.model.ApplicationIdentity;

import java.util.List;
import java.util.regex.Pattern;

public interface CloudFoundryApiService {

    void bindApplications(String serviceInstanceId, List<ApplicationIdentity> application) throws CloudFoundryException;

    void bindRoutes(String serviceInstanceId, List<String> routeIds) throws CloudFoundryException;

    ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException;

    String getApplicationState(String applicationUuid) throws CloudFoundryException;

    String getHost(String routeId) throws CloudFoundryException;

    boolean isAppRunning(String appUid) throws CloudFoundryException;

    List<String/**ids**/> listApplicationRoutes(String applicationUuid) throws CloudFoundryException;

    List<ApplicationIdentity> listApplications(String spaceUuid, Pattern excludeNames) throws CloudFoundryException;

    List<String/**ids**/> listRouteApplications(String routeUuid) throws CloudFoundryException;

    boolean startApplication(String applicationUuid) throws CloudFoundryException;

    boolean stopApplication(String applicationUuid) throws CloudFoundryException;

    void unbind(String bindingId) throws CloudFoundryException;

}
