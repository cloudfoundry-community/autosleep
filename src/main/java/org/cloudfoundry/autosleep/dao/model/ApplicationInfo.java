package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.util.EqualUtil;
import org.cloudfoundry.autosleep.util.Serializers;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Slf4j
@JsonAutoDetect()
public class ApplicationInfo {

    @JsonSerialize
    private UUID uuid;
    @JsonSerialize
    private String name;
    @JsonSerialize
    private String space;
    @JsonSerialize
    private String organization;
    @JsonSerialize
    private CloudApplication.AppState state;
    @JsonSerialize
    private int runningInstances;
    @JsonSerialize
    private int instances;
    @JsonSerialize
    private List<String> uris;

    @JsonSerialize(using = Serializers.InstantSerializer.class)
    @JsonDeserialize(using = Serializers.InstantDeserializer.class)
    private Instant lastEvent;

    @JsonSerialize(using = Serializers.InstantSerializer.class)
    @JsonDeserialize(using = Serializers.InstantDeserializer.class)
    private Instant lastLog;

    @JsonSerialize(using = Serializers.InstantSerializer.class)
    @JsonDeserialize(using = Serializers.InstantDeserializer.class)
    private Instant nextCheck;

    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private ApplicationInfo() {
    }

    public ApplicationInfo(CloudApplication application, UUID uuid, Instant lastEvent, Instant lastLog) {
        this.uuid = uuid;
        this.name = application.getName();

        if (application.getSpace() != null ) {
            this.space = application.getSpace().getName();
            if (application.getSpace().getOrganization() != null) {
                this.organization = application.getSpace().getOrganization().getName();
            }
        }
        this.state = application.getState();
        this.runningInstances = application.getRunningInstances();
        this.instances = application.getInstances();
        this.uris = application.getUris();

        this.lastEvent = lastEvent;
        this.lastLog = lastLog;
    }

    /**
     * Return which ever date is the most recent (between last deploy event and last log).
     *
     * @return Most recent date
     */
    @JsonIgnore
    public Instant computeLastDate() {
        if (lastLog == null) {
            if (lastEvent == null) {
                // from what we understood, events will always be returned, whereas recent logs may be empty.
                log.error("Last event is not supposed to be null");
                return null;
            }
            return lastEvent;
        } else if (lastEvent == null) {
            log.error("Last event is not supposed to be null");
            return lastLog;
        } else {
            log.debug("computeLastDate - lastEvent.isAfter(lastLog) = {}", lastEvent.isAfter(lastLog));
            return lastEvent.isAfter(lastLog) ? lastEvent : lastLog;
        }
    }


    @Override
    public String toString() {
        return "[ApplicationInfo:" + getName() + "/" + getUuid() + " lastEvent:"
                + getLastEvent() + " lastLog:" + getLastLog() + "]";
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
                && EqualUtil.areEquals(this.getSpace(), other.getSpace())
                && EqualUtil.areEquals(this.getOrganization(), other.getOrganization())
                && EqualUtil.areEquals(this.getInstances(), other.getInstances())
                && EqualUtil.areEquals(this.getLastLog(), other.getLastLog())
                && EqualUtil.areEquals(this.getLastEvent(), other.getLastEvent())
                && EqualUtil.areEquals(this.getRunningInstances(), other.getRunningInstances())
                && EqualUtil.areEquals(this.getState(), other.getState())
                && EqualUtil.areEquals(this.getUris(), other.getUris())
                && EqualUtil.areEquals(this.getInstances(), other.getInstances());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }


}
