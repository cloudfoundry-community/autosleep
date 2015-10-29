package org.cloudfoundry.autosleep.admin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceBinding {
    private String appGuid;

    private String id;
}
