package org.cloudfoundry.autosleep.client.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeAppRequest {
    private String state;
}
