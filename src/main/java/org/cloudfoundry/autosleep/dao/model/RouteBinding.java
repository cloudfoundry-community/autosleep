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
public class RouteBinding {

    @Id
    private String bindingId;

    private String configurationId;

    private String routeId;

    private String linkedApplicationId;

    private String linkedApplicationBindingId;

    private String localRoute;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : [id:" + bindingId + " serviceId:+" + configurationId
                + " app:" + linkedApplicationId +  " routeId:" + routeId + " localRoute " + localRoute + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof RouteBinding)) {
            return false;
        } else {
            RouteBinding other = (RouteBinding) object;
            return Objects.equals(bindingId, other.bindingId)
                    && Objects.equals(configurationId, other.configurationId)
                    && Objects.equals(routeId, other.routeId);
        }
    }

    @Override
    public int hashCode() {
        return bindingId.hashCode();
    }
}
