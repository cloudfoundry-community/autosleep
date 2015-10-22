package org.cloudfoundry.autosleep.remote;

import java.util.List;

public interface IRemote {

    ApplicationInfo getApplicationInfo(String appGuid);

    boolean stopApplication(String appGuid);

    boolean startApplication(String appGuid);

    List<String> getApplicationsNames();
}
