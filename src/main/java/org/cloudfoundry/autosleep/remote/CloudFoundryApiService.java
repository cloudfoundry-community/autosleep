package org.cloudfoundry.autosleep.remote;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public interface CloudFoundryApiService {

    ApplicationActivity getApplicationActivity(UUID appUid);


    void stopApplication(UUID applicationUuid);

    void startApplication(UUID applicationUuid);

    List<ApplicationIdentity> listApplications(UUID spaceUuid, Pattern excludeNames);

    void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId);

    void bindServiceInstance(List<ApplicationIdentity> application, String serviceInstanceId);
}
