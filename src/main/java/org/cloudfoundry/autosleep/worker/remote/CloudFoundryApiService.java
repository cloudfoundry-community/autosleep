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

package org.cloudfoundry.autosleep.worker.remote;

import org.cloudfoundry.autosleep.worker.remote.model.ApplicationActivity;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;

import java.util.List;
import java.util.regex.Pattern;

public interface CloudFoundryApiService {

    void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId) throws CloudFoundryException;

    void bindServiceInstance(List<ApplicationIdentity> application, String serviceInstanceId) throws
            CloudFoundryException;

    void bindServiceToRoute(String serviceInstanceId, String routeId) throws CloudFoundryException;

    ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException;

    List<String/**ids**/> listApplicationRoutes(String applicationUuid) throws CloudFoundryException;

    List<String/**ids**/> listRouteApplications(String routeUuid) throws CloudFoundryException;

    List<ApplicationIdentity> listApplications(String spaceUuid, Pattern excludeNames) throws CloudFoundryException;

    void startApplication(String applicationUuid) throws CloudFoundryException;

    void stopApplication(String applicationUuid) throws CloudFoundryException;

    void unbind(String bindingId) throws CloudFoundryException;

}
