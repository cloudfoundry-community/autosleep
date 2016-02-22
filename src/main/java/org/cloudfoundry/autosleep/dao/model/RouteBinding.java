/**
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    private String linkedApplicationBindingId;

    private String linkedApplicationId;

    private String localRoute;

    private String routeId;

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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : [id:" + bindingId + " serviceId:+" + configurationId
                + " app:" + linkedApplicationId + " routeId:" + routeId + " localRoute " + localRoute + "]";
    }

}
