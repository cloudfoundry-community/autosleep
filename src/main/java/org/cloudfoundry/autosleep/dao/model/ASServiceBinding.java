package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.util.EqualUtil;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Slf4j
@JsonAutoDetect()
public class ASServiceBinding extends ServiceInstanceBinding {


    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private ASServiceBinding() {
        super(null, null, null, null, null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ASServiceBinding(String id, String serviceInstanceId, Map<String, Object> credentials, String
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
        if (!(object instanceof ASServiceBinding)) {
            return false;
        }
        ASServiceBinding other = (ASServiceBinding) object;

        return EqualUtil.areEquals(this.getId(), other.getId())
                && EqualUtil.areEquals(this.getServiceInstanceId(), other.getServiceInstanceId())
                && EqualUtil.areEquals(this.getAppGuid(), other.getAppGuid())
                && EqualUtil.areEquals(this.getCredentials(), other.getCredentials())
                && EqualUtil.areEquals(this.getSyslogDrainUrl(), other.getSyslogDrainUrl());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
