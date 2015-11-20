package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.util.EqualUtil;
import org.cloudfoundry.autosleep.util.serializer.InstantDeserializer;
import org.cloudfoundry.autosleep.util.serializer.InstantSerializer;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

import java.time.Instant;
import java.util.UUID;

@Getter
@Slf4j
@JsonAutoDetect()
public class ApplicationInfo {

    @JsonSerialize
    private UUID uuid;

    @JsonSerialize
    private String name;

    @JsonSerialize
    private CloudApplication.AppState appState;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant lastEvent;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant lastLog;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant nextCheck;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant lastCheck;

    @JsonSerialize
    private ApplicationStateMachine stateMachine = new ApplicationStateMachine();

    @JsonSerialize
    private String serviceInstanceId;

    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private ApplicationInfo() {
    }

    public ApplicationInfo(UUID uuid, String serviceId) {
        this.uuid = uuid;
        this.serviceInstanceId = serviceId;
    }

    public ApplicationInfo withRemoteInfo(ApplicationActivity activity) {
        updateRemoteInfo(activity);
        return this;
    }

    public void updateRemoteInfo(ApplicationActivity activity) {
        this.appState = activity.getState();
        this.lastEvent = activity.getLastEvent();
        this.lastLog = activity.getLastLog();
        this.name = activity.getApplication().getName();
    }

    public void markAsChecked(Instant next) {
        this.lastCheck = Instant.now();
        this.nextCheck = next;
    }

    public void clearCheckInformation() {
        this.lastCheck = Instant.now();
        this.nextCheck = null;
        this.appState = null;
    }

    public void markAsPutToSleep() {
        this.appState = AppState.STOPPED;
        this.lastEvent = Instant.now();
    }


    @Override
    public String toString() {
        return "[ApplicationInfo:" + getName() + "/" + getUuid() + " lastEvent:"
                + getLastEvent() + " lastLog:" + getLastLog() + " serviceId:" + getServiceInstanceId() + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ApplicationInfo)) {
            return false;
        }
        ApplicationInfo other = (ApplicationInfo) object;

        return EqualUtil.areEquals(this.getUuid(), other.getUuid())
                && EqualUtil.areEquals(this.getName(), other.getName())
                && EqualUtil.areEquals(this.getLastLog(), other.getLastLog())
                && EqualUtil.areEquals(this.getLastEvent(), other.getLastEvent())
                && EqualUtil.areEquals(this.getLastCheck(), other.getLastCheck())
                && EqualUtil.areEquals(this.getNextCheck(), other.getNextCheck())
                && EqualUtil.areEquals(this.getStateMachine(), other.getStateMachine())
                && EqualUtil.areEquals(this.getAppState(), other.getAppState())
                && EqualUtil.areEquals(this.getServiceInstanceId(), other.getServiceInstanceId());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }


}
