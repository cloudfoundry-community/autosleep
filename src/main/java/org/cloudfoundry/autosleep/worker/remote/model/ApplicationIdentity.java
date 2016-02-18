package org.cloudfoundry.autosleep.worker.remote.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationIdentity {

    private String guid;

    private String name;

}
