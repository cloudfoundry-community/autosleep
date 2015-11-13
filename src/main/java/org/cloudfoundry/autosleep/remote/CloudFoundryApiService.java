package org.cloudfoundry.autosleep.remote;

import java.util.List;
import java.util.UUID;

public interface CloudFoundryApiService {

    ApplicationActivity getApplicationActivity(UUID appUid);


    void stopApplication(UUID appUid);

    void startApplication(UUID appUid);

    List<UUID> listApplications(UUID spaceUuid, String excludeNamesExpression);
}
