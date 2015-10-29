package org.cloudfoundry.autosleep.admin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstance {
    private String instanceId;

    private String definitionId;

    private String planId;

}
