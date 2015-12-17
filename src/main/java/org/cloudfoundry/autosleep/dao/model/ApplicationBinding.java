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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Slf4j
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ApplicationBinding {
    private String serviceBindingId;

    private String serviceInstanceId;

    private String applicationId;


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
    }

    @Override
    public int hashCode() {
        return serviceBindingId.hashCode();
    }
}
