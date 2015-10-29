package org.cloudfoundry.autosleep.servicebroker.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.util.Map;

@Getter
@Setter
@Slf4j
@JsonAutoDetect()
public class AutoSleepServiceBinding extends ServiceInstanceBinding {

    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private AutoSleepServiceBinding() {
        super(null,null,null,null,null);
    }

    public AutoSleepServiceBinding(String id, String serviceInstanceId, Map<String, Object> credentials, String
            syslogDrainUrl, String appGuid) {
        super(id, serviceInstanceId, credentials, syslogDrainUrl, appGuid);
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
        if (!(object instanceof AutoSleepServiceBinding)) {
            return false;
        }
        AutoSleepServiceBinding other = (AutoSleepServiceBinding) object;
        return !(this.getId() == null ? other.getId() != null : !this.getId().equals(other.getId())) && !(this
                .getServiceInstanceId() == null ? other.getServiceInstanceId() != null : !this.getServiceInstanceId()
                .equals(other.getServiceInstanceId())) && !(this.getSyslogDrainUrl() == null ? other
                .getSyslogDrainUrl() != null : !this.getSyslogDrainUrl().equals(other.getSyslogDrainUrl())) && !(
                this
                .getAppGuid() == null ? other.getAppGuid() != null : !this.getAppGuid().equals(other.getAppGuid()))
                && !(this.getCredentials() == null ? other.getCredentials() != null : !this.getCredentials().equals(
                other.getCredentials()));

    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = 1;
        result = result * prime + getServiceInstanceId().hashCode();
        result = result * prime + getId().hashCode();
        result = result * prime + getCredentials().hashCode();
        result = result * prime + getSyslogDrainUrl().hashCode();
        result = result * prime + getAppGuid().hashCode();
        return result;
    }
}
