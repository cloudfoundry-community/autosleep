package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Slf4j
@JsonAutoDetect()
public class ApplicationBinding extends ServiceInstanceBinding {


    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private ApplicationBinding() {
        super(null, null, null, null, null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ApplicationBinding(String id, String serviceInstanceId, Map<String, Object> credentials, String
            syslogDrainUrl, String appGuid) {
        super(id, serviceInstanceId, credentials, syslogDrainUrl, appGuid);
        //will throw an exception if wrong format TODO check if needed with new java-client-lib
        UUID.fromString(appGuid);
    }

    @Override
    public String toString() {
        return "AutoSleepSB:[id:" + getId() + " serviceId:+" + getServiceInstanceId()
                + " syslogUrl:" + getSyslogDrainUrl() + " app:" + getAppGuid() + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ApplicationBinding)) {
            return false;
        }
        ApplicationBinding other = (ApplicationBinding) object;

        return Objects.equals(this.getId(), other.getId())
                && Objects.equals(this.getServiceInstanceId(), other.getServiceInstanceId())
                && Objects.equals(this.getAppGuid(), other.getAppGuid())
                && Objects.equals(this.getCredentials(), other.getCredentials())
                && Objects.equals(this.getSyslogDrainUrl(), other.getSyslogDrainUrl());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
