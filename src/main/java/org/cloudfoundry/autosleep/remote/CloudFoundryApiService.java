package org.cloudfoundry.autosleep.remote;

import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;

import java.util.List;
import java.util.UUID;

public interface CloudFoundryApiService {

    ApplicationInfo getApplicationInfo(UUID appUid);


    void stopApplication(UUID appUid);

    void startApplication(UUID appUid);

    List<UUID> listApplications(UUID spaceUuid, String excludeNamesExpression);
}
