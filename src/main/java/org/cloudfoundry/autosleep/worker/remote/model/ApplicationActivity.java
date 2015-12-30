package org.cloudfoundry.autosleep.worker.remote.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

@Getter
@AllArgsConstructor
public class ApplicationActivity {

    private ApplicationIdentity application;

    private AppState state;

    private ApplicationInfo.DiagnosticInfo.ApplicationEvent lastEvent;

    private ApplicationInfo.DiagnosticInfo.ApplicationLog lastLog;


}
