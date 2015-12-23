package org.cloudfoundry.autosleep.worker.remote;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationIdentity {
    private String guid;

    private String name;
}
