package org.cloudfoundry.autosleep.worker.remote.model;

import lombok.Builder;
import lombok.Getter;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;

@Getter
@Builder
public class ApplicationActivity {

    private ApplicationIdentity application;

    private ApplicationInfo.DiagnosticInfo.ApplicationEvent lastEvent;

    private ApplicationInfo.DiagnosticInfo.ApplicationLog lastLog;

    private String state;

}
