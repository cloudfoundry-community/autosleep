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

package org.cloudfoundry.autosleep.access.dao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@ToString(of = {"uuid", "name", "diagnosticInfo"})
public class ApplicationInfo {

    @Getter
    @Slf4j
    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @ToString(of = {"appState", "lastCheck", "lastEvent", "lastLog", "nextCheck"})
    public static class DiagnosticInfo {

        @Getter
        @Setter
        @Slf4j
        @Embeddable
        @EqualsAndHashCode
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @ToString(of={"timestamp", "type"})
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

            @Builder
            ApplicationEvent(String actee,
                             String actor,
                             String name,
                             long timestamp,
                             String type) {
                this.actee = actee;
                this.actor = actor;
                this.name = name;
                this.timestamp = Instant.ofEpochMilli(timestamp);
                this.type = type;
            }

        }

        @Getter
        @Slf4j
        @Embeddable
        @EqualsAndHashCode
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @ToString(of={"timestamp", "messageType", "sourceId", "sourceName"})
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

            @Builder
            ApplicationLog(String message,
                           String messageType,
                           String sourceId,
                           String sourceName,
                           long timestamp) {
                this.setMessage(message);
                this.messageType = messageType;
                this.sourceId = sourceId;
                this.sourceName = sourceName;
                this.timestamp = Instant.ofEpochMilli(timestamp);
            }

            private void setMessage(String message) {
                this.message = StringUtils.abbreviate(message,254);
            }
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

        @Builder
        DiagnosticInfo(String appState,
                       long lastCheck,
                       ApplicationEvent lastEvent,
                       ApplicationLog lastLog,
                       long nextCheck) {
            this.appState = appState;
            this.lastCheck = Instant.ofEpochMilli(lastCheck);
            this.lastEvent = lastEvent;
            this.lastLog = lastLog;
            this.nextCheck = Instant.ofEpochMilli(nextCheck);
        }
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
        this.diagnosticInfo = new DiagnosticInfo();
        this.enrollmentState = new EnrollmentState();
    }

    @Builder
    ApplicationInfo(DiagnosticInfo diagnosticInfo,
                    EnrollmentState enrollmentState,
                    String name,
                    String uuid) {
        this.diagnosticInfo = diagnosticInfo == null ? DiagnosticInfo.builder().build() : diagnosticInfo;
        this.enrollmentState = enrollmentState == null ? new EnrollmentState() : enrollmentState;
        this.name = name;
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
        this.diagnosticInfo.lastEvent = DiagnosticInfo.ApplicationEvent.builder()
                .actor("autosleep")
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public void updateDiagnosticInfo(DiagnosticInfo.ApplicationLog lastLog,
                                     DiagnosticInfo.ApplicationEvent lastEvent,
                                     String name,
                                     String state) {
        this.diagnosticInfo.lastLog = lastLog;
        this.diagnosticInfo.lastEvent = lastEvent;
        this.name = name;
        this.diagnosticInfo.appState = state;
    }

}
