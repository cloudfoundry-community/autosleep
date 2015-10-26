package org.cloudfoundry.autosleep.remote;

import java.util.List;

public interface CloudFoundryApi {

    ApplicationInfo getApplicationInfo(String appName);

    void stopApplication(String appName);

    void startApplication(String appName);

    List<String> getApplicationsNames();
}
