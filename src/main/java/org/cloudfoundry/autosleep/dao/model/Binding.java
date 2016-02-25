package org.cloudfoundry.autosleep.dao.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Binding {

    public enum ResourceType {
        Application, Route
    }

    private String resourceId;

    private ResourceType resourceType;

    @Id
    private String serviceBindingId;

    private String serviceInstanceId;

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof Binding)) {
            return false;
        } else {
            Binding other = (Binding) object;
            return Objects.equals(serviceBindingId, other.serviceBindingId)
                    && Objects.equals(serviceInstanceId, other.serviceInstanceId);
        }
    }

    @Override
    public int hashCode() {
        return serviceBindingId.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : [id:" + serviceBindingId + " serviceId:+" + serviceInstanceId
                + " app:" + resourceId + "]";
    }
}
