package org.cloudfoundry.autosleep.admin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServiceBinding {
    private String appGuid;

    private String id;
}
