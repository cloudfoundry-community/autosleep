package org.cloudfoundry.autosleep.remote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ApplicationActivity {

    private UUID guid;

    private String name;

    private AppState state;

    private Instant lastEvent;

    private Instant lastLog;


}
