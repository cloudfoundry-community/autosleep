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

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.RouteBinding;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class BeanGenerator {

    public static final UUID ORG_TEST = UUID.randomUUID();

    public static final UUID PLAN_ID = UUID.randomUUID();

    public static final UUID SERVICE_DEFINITION_ID = UUID.randomUUID();

    public static final UUID SPACE_TEST = UUID.randomUUID();

    public static ApplicationIdentity createAppIdentity(String appUuid) {
        return ApplicationIdentity.builder()
                .guid(appUuid)
                .name(appUuid + "-name")
                .build();
    }

    public static ApplicationInfo createAppInfo() {
        return createAppInfoLinkedToService(null, null);
    }

    public static ApplicationInfo createAppInfoLinkedToService(String serviceId) {
        return createAppInfoLinkedToService(null, serviceId);
    }

    public static ApplicationInfo createAppInfoLinkedToService(String appUuid, String serviceId) {
        if (appUuid == null) {
            appUuid = UUID.randomUUID().toString();
        }
        ApplicationInfo applicationInfo = new ApplicationInfo(appUuid);
        if (serviceId != null) {
            applicationInfo.getEnrollmentState().addEnrollmentState(serviceId);
        }
        return applicationInfo;
    }

    public static ApplicationInfo createAppInfoWithDiagnostic(String appUuid, String name, String state) {
        return createAppInfoWithDiagnostic(appUuid, name, state, null, null);
    }

    public static ApplicationInfo createAppInfoWithDiagnostic(String appUuid, String name, String state, Instant
            lastLogDate, Instant lastEventDate) {
        ApplicationInfo applicationInfo = new ApplicationInfo(appUuid);
        applicationInfo.updateDiagnosticInfo(state,
                createAppLog(lastLogDate),
                createCloudEvent(lastEventDate),
                name);
        return applicationInfo;
    }

    public static ApplicationInfo.DiagnosticInfo.ApplicationLog createAppLog(Instant instant) {
        return ApplicationInfo.DiagnosticInfo.ApplicationLog.builder()
                .message("fakelog")
                .timestamp(instant != null ? instant : Instant.now())
                .messageType("STDOUT")
                .sourceName("sourceName")
                .sourceId("sourceId")
                .build();
    }

    public static ApplicationInfo.DiagnosticInfo.ApplicationLog createAppLog() {
        return createAppLog(null);
    }

    public static ApplicationBinding createBinding(String serviceId, String bindingId, String appId) {
        if (serviceId == null) {
            serviceId = UUID.randomUUID().toString();
        }
        if (bindingId == null) {
            bindingId = UUID.randomUUID().toString();
        }
        if (appId == null) {
            appId = UUID.randomUUID().toString();
        }
        return ApplicationBinding.builder().serviceBindingId(bindingId)
                .serviceInstanceId(serviceId).applicationId(appId).build();
    }

    public static ApplicationBinding createBinding() {
        return createBinding(null, null, null);
    }

    public static ApplicationInfo.DiagnosticInfo.ApplicationEvent createCloudEvent(Instant instant) {
        ApplicationInfo.DiagnosticInfo.ApplicationEvent event =
                new ApplicationInfo.DiagnosticInfo.ApplicationEvent("fakeEvent");
        event.setTimestamp(instant != null ? instant : Instant.now());
        return event;
    }

    public static ApplicationInfo.DiagnosticInfo.ApplicationEvent createCloudEvent() {
        return createCloudEvent(null);
    }

    public static RouteBinding createRouteBinding(String bindingId,
                                                  String serviceId,
                                                  String linkedAppId,
                                                  String linkedAppBindingId) {
        return RouteBinding.builder()
                .bindingId(bindingId)
                .routeId("aRouteId")
                .configurationId(serviceId)
                .localRoute("alocalroute")
                .linkedApplicationId(linkedAppId)
                .linkedApplicationBindingId(linkedAppBindingId).build();
    }

    public static RouteBinding createRouteBinding(String bindingId) {
        return createRouteBinding(bindingId, "", "", "");
    }

    public static SpaceEnrollerConfig createServiceInstance() {
        return createServiceInstance(UUID.randomUUID().toString());
    }

    public static SpaceEnrollerConfig createServiceInstance(String serviceId) {
        return SpaceEnrollerConfig.builder()
                .serviceDefinitionId(SERVICE_DEFINITION_ID.toString())
                .planId(PLAN_ID.toString())
                .organizationId(ORG_TEST.toString())
                .spaceId(SPACE_TEST.toString())
                .id(serviceId).build();
    }

    public static String getSampleVcapApplication(UUID applicationId, String applicationName, String... uris) {
        return "{\"limits\":{\"mem\":1024,\"disk\":1024,\"fds\":16384},"
                + "\"application_id\":\"" + applicationId.toString() + "\","
                + "\"application_version\":\"b546c9d4-8885-4d50-a855-490ddb5b5a1c\","
                + "\"application_name\":\"" + applicationName + "\","
                + "\"application_uris\":["
                + String.join(", ",
                Arrays.asList(uris).stream()
                        .map(uri -> "\"" + uri + "\"")
                        .collect(Collectors.toList()))
                + "],"
                + " \"version\":\"b546c9d4-8885-4d50-a855-490ddb5b5a1c\","
                + "\"name\":\"autosleep-app\","
                + "\"space_name\":\"autosleep\""
                + ",\"space_id\":\"2d745a4b-67e3-4398-986e-2adbcf8f7ec9\","
                + "\"uris\":[\"autosleep-app-ben.cf.ns.nd-paas.itn.ftgroup\","
                + "\"autosleep-nonnational-artotype.cf.ns.nd-paas.itn.ftgroup\","
                + "\"autosleep.cf.ns.nd-paas.itn.ftgroup\"]"
                + ",\"users\":null,"
                + "\"instance_id\":\"7984a682cab9447891674f862299c77f\","
                + "\"instance_index\":0,"
                + "\"host\":\"0.0.0.0\","
                + "\"port\":61302,"
                + "\"started_at\":\"2015-11-18 15:49:06 +0000\","
                + "\"started_at_timestamp\":1447861746,"
                + "\"start\":\"2015-11-18 15:49:06 +0000\","
                + "\"state_timestamp\":1447861746"
                + "}";
    }
}
