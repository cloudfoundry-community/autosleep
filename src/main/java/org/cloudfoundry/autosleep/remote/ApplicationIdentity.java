package org.cloudfoundry.autosleep.remote;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ApplicationIdentity {
    private UUID guid;

    private String name;
}
