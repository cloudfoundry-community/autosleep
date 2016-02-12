package org.cloudfoundry.autosleep.worker.remote;

import org.cloudfoundry.autosleep.worker.remote.model.ApplicationActivity;
import org.cloudfoundry.autosleep.worker.remote.model.ApplicationIdentity;

import java.util.List;
import java.util.regex.Pattern;

public interface CloudFoundryApiService {

    void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId) throws CloudFoundryException;

    void bindServiceInstance(List<ApplicationIdentity> application, String serviceInstanceId) throws
            CloudFoundryException;

    ApplicationActivity getApplicationActivity(String appUid) throws CloudFoundryException;

    List<ApplicationIdentity> listApplications(String spaceUuid, Pattern excludeNames) throws CloudFoundryException;

    void startApplication(String applicationUuid) throws CloudFoundryException;

    void stopApplication(String applicationUuid) throws CloudFoundryException;
}
