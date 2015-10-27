package org.cloudfoundry.autosleep.remote;

import java.util.List;
import java.util.UUID;

public interface CloudFoundryApiService {

    ApplicationInfo getApplicationInfo(UUID appUid);

    void stopApplication(UUID appUid);

    void startApplication(UUID appUid);

    List<String> getApplicationsNames();
}
