package org.cloudfoundry.autosleep.worker.remote.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationIdentity {
    private String guid;

    private String name;
}
