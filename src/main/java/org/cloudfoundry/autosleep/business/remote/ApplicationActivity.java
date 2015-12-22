package org.cloudfoundry.autosleep.business.remote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ApplicationActivity {

    private ApplicationIdentity application;

    private AppState state;

    private Instant lastEvent;

    private Instant lastLog;


}
