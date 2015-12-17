package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import javax.persistence.Entity;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Slf4j
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Entity
public class ApplicationBinding {
    private String serviceBindingId;

    private String serviceInstanceId;

    private String applicationId;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ApplicationBinding(String id, String serviceInstanceId, Map<String, Object> credentials, String
            syslogDrainUrl, String appGuid) {
        super(id, serviceInstanceId, credentials, syslogDrainUrl, appGuid);
        //will throw an exception if wrong format TODO check if needed with new java-client-lib
        UUID.fromString(appGuid);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : [id:" + serviceBindingId + " serviceId:+" + serviceInstanceId
                + " app:" + applicationId + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof ApplicationBinding)) {
            return false;
        } else {
            ApplicationBinding other = (ApplicationBinding) object;
            return Objects.equals(serviceBindingId, other.serviceBindingId)
                    && Objects.equals(serviceInstanceId, other.serviceInstanceId);
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
        return serviceBindingId.hashCode();
    }
}
