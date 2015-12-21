package org.cloudfoundry.autosleep.dao.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;

@Getter
@Setter
@Slf4j
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Entity
public class ApplicationBinding {

    @Id
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
