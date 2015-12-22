package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.util.serializer.InstantDeserializer;
import org.cloudfoundry.autosleep.util.serializer.InstantSerializer;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;

@Getter
@Slf4j
@Entity
public class ApplicationInfo {

    @Getter
    @Slf4j
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Embeddable
    public static class DiagnosticInfo {
        @JsonSerialize(using = InstantSerializer.class)
        @JsonDeserialize(using = InstantDeserializer.class)
        private Instant lastEvent;

        @JsonSerialize(using = InstantSerializer.class)
        @JsonDeserialize(using = InstantDeserializer.class)
        private Instant lastLog;

        @JsonSerialize
        private CloudApplication.AppState appState;

        @JsonSerialize(using = InstantSerializer.class)
        @JsonDeserialize(using = InstantDeserializer.class)
        private Instant nextCheck;

        @JsonSerialize(using = InstantSerializer.class)
        @JsonDeserialize(using = InstantDeserializer.class)
        private Instant lastCheck;
    }

    @Getter
    @Slf4j
    @Embeddable
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

        private HashMap<String /**serviceId.**/, EnrollmentState.State> states;

        private EnrollmentState(){
            states = new HashMap<>();
        }

        public void addEnrollmentState(String serviceId) {
            states.put(serviceId, EnrollmentState.State.ENROLLED);
        }

        public void updateEnrollment(String serviceId, boolean addToBlackList) {
            if (addToBlackList) {
                states.put(serviceId, EnrollmentState.State.BLACKLISTED);
            } else {
                states.remove(serviceId);
            }
        }

        public boolean isWatched() {
            return states.values().stream().filter(
                    serviceInstanceState -> serviceInstanceState == EnrollmentState.State.ENROLLED
            ).findAny().isPresent();
        }

        public boolean isCandidate(String serviceInstanceId) {
            return !states.containsKey(serviceInstanceId);
        }

        public boolean isEnrolledByService(String serviceInstanceId) {
            return states.get(serviceInstanceId) == EnrollmentState.State.ENROLLED;
        }

    }

    @Id
    @Column(length = 40)
    private String uuid;

    private String name;

    @Embedded
    @JsonUnwrapped
    private DiagnosticInfo diagnosticInfo;


    @Embedded
    @JsonUnwrapped
    private EnrollmentState enrollmentState;



    private ApplicationInfo(){
        diagnosticInfo = new DiagnosticInfo();
        enrollmentState = new EnrollmentState();
    }


    public ApplicationInfo(String uuid) {
        this();
        this.uuid = uuid;
    }

    public ApplicationInfo withRemoteInfo(ApplicationActivity activity) {
        updateDiagnosticInfo(activity);
        return this;
    }



    public void updateDiagnosticInfo(ApplicationActivity activity) {
        this.diagnosticInfo.appState = activity.getState();
        this.diagnosticInfo.lastLog = activity.getLastLog();
        this.diagnosticInfo.lastEvent = activity.getLastEvent();
        this.name = activity.getApplication().getName();
    }

    public void markAsChecked(Instant next) {
        this.diagnosticInfo.lastCheck = Instant.now();
        this.diagnosticInfo.nextCheck = next;
    }

    public void clearCheckInformation() {
        this.diagnosticInfo.lastCheck = Instant.now();
        this.diagnosticInfo.nextCheck = null;
        this.diagnosticInfo.appState = null;
    }

    public void markAsPutToSleep() {
        this.diagnosticInfo.appState = AppState.STOPPED;
        this.diagnosticInfo.lastEvent = Instant.now();
    }


    @Override
    public String toString() {
        return "[ApplicationInfo:" + name+ "/" + uuid + " lastEvent:"
                + diagnosticInfo.lastEvent + " lastLog:" + diagnosticInfo.lastLog + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof ApplicationInfo)) {
            return false;
        } else {
            ApplicationInfo other = (ApplicationInfo) object;
            return Objects.equals(uuid, other.uuid) && Objects.equals(name, other.name);
        }

    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }


}
