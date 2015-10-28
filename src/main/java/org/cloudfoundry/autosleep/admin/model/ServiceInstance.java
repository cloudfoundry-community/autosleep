package org.cloudfoundry.autosleep.admin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServiceInstance {
    private String instanceId;

    private String definitionId;

    private String planId;

}
