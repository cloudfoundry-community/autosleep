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

package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config.CloudFoundryAppState;
import org.cloudfoundry.autosleep.util.serializer.InstantDeserializer;
import org.cloudfoundry.autosleep.util.serializer.InstantSerializer;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;
import java.util.HashMap;

@Getter
@Slf4j
@Entity
@EqualsAndHashCode
public class ApplicationInfo {

    @Getter
    @Slf4j
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Embeddable
    @EqualsAndHashCode
    public static class DiagnosticInfo {

        @Getter
        @Setter
        @Slf4j
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @Embeddable
        @EqualsAndHashCode
        public static class ApplicationEvent {

            @JsonSerialize
            @Column(name = "event_actee")
            private String actee;

            @JsonSerialize
            @Column(name = "event_actor")
            private String actor;

            @JsonSerialize
            @Column(name = "event_name")
            private String name;

            @JsonSerialize(using = InstantSerializer.class)
            @JsonDeserialize(using = InstantDeserializer.class)
            @Column(name = "event_time")
            private Instant timestamp;

            @JsonSerialize
            @Column(name = "event_type")
            private String type;

            public ApplicationEvent(String name) {
                this.name = name;
            }
        }

        @Getter
        @Slf4j
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        @Builder
        @Embeddable
        @EqualsAndHashCode
        public static class ApplicationLog {

            @JsonSerialize
            @Column(name = "log_message")
            private String message;

            @JsonSerialize
            @Column(name = "log_message_type")
            private String messageType;

            @JsonSerialize
            @Column(name = "log_source_id")
            private String sourceId;

            @JsonSerialize
            @Column(name = "log_source_name")
            private String sourceName;

            @JsonSerialize(using = InstantSerializer.class)
            @JsonDeserialize(using = InstantDeserializer.class)
            @Column(name = "log_time")
            private Instant timestamp;

        }

        @JsonSerialize
        private String appState;

        //see https://issues.jboss.org/browse/HIBERNATE-50
        @JsonIgnore
        private int hibernateWorkaround = 1;

        @JsonSerialize(using = InstantSerializer.class)
        @JsonDeserialize(using = InstantDeserializer.class)
        private Instant lastCheck;

        @Embedded
        @JsonSerialize
        private ApplicationEvent lastEvent;

        @Embedded
        @JsonSerialize
        private ApplicationLog lastLog;

        @JsonSerialize(using = InstantSerializer.class)
        @JsonDeserialize(using = InstantDeserializer.class)
        private Instant nextCheck;
    }

    @Getter
    @Slf4j
    @Embeddable
    @EqualsAndHashCode
    public static class EnrollmentState {

        public enum State {
            /**
             * service instance is bound to the application.
             */
            ENROLLED,
            /**
             * service (with AUTO_ENROLLMENT set to false) was manually unbound,
             * it won't be automatically bound again.
             */
            BLACKLISTED
        }

        @Column(length = 300) //to force BLOB type and not TINYBLOB
        private HashMap<String /**serviceId.**/, EnrollmentState.State> states;

        private EnrollmentState() {
            states = new HashMap<>();
        }

        public void addEnrollmentState(String serviceId) {
            states.put(serviceId, EnrollmentState.State.ENROLLED);
        }

        public boolean isCandidate(String serviceInstanceId) {
            return !states.containsKey(serviceInstanceId);
        }

        public boolean isEnrolledByService(String serviceInstanceId) {
            return states.get(serviceInstanceId) == EnrollmentState.State.ENROLLED;
        }

        public boolean isWatched() {
            return states.values().stream().filter(
                    serviceInstanceState -> serviceInstanceState == EnrollmentState.State.ENROLLED
            ).findAny().isPresent();
        }

        public void updateEnrollment(String serviceId, boolean addToBlackList) {
            if (addToBlackList) {
                states.put(serviceId, EnrollmentState.State.BLACKLISTED);
            } else {
                states.remove(serviceId);
            }
        }

    }

    @Embedded
    @JsonSerialize
    private DiagnosticInfo diagnosticInfo;

    @Embedded
    @JsonUnwrapped
    private EnrollmentState enrollmentState;

    private String name;

    @Id
    @Column(length = 40)
    private String uuid;

    private ApplicationInfo() {
        diagnosticInfo = new DiagnosticInfo();
        enrollmentState = new EnrollmentState();
    }

    public ApplicationInfo(String uuid) {
        this();
        this.uuid = uuid;
    }

    public void clearCheckInformation() {
        this.diagnosticInfo.lastCheck = Instant.now();
        this.diagnosticInfo.nextCheck = null;
        this.diagnosticInfo.appState = null;
    }

    public void markAsChecked(Instant next) {
        this.diagnosticInfo.lastCheck = Instant.now();
        this.diagnosticInfo.nextCheck = next;
    }

    public void markAsPutToSleep() {
        this.diagnosticInfo.appState = CloudFoundryAppState.STOPPED;
        DiagnosticInfo.ApplicationEvent applicationEvent = new DiagnosticInfo.ApplicationEvent("Autosleep-stop");
        applicationEvent.setActor("autosleep");
        applicationEvent.setTimestamp(Instant.now());
        this.diagnosticInfo.lastEvent = applicationEvent;
    }

    @Override
    public String toString() {
        return "[ApplicationInfo:" + name + "/" + uuid + " lastEvent:"
                + diagnosticInfo.lastEvent + " lastLog:" + diagnosticInfo.lastLog + "]";
    }

    public void updateDiagnosticInfo(String state, DiagnosticInfo.ApplicationLog lastLog, DiagnosticInfo
            .ApplicationEvent lastEvent, String name) {
        this.diagnosticInfo.appState = state;
        this.diagnosticInfo.lastLog = lastLog;
        this.diagnosticInfo.lastEvent = lastEvent;
        this.name = name;
    }

}
