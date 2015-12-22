package org.cloudfoundry.autosleep.business.remote;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public interface CloudFoundryApiService {

    ApplicationActivity getApplicationActivity(UUID appUid) throws EntityNotFoundException, CloudFoundryException;


    void stopApplication(UUID applicationUuid) throws EntityNotFoundException, CloudFoundryException;

    void startApplication(UUID applicationUuid) throws EntityNotFoundException, CloudFoundryException;

    List<ApplicationIdentity> listApplications(UUID spaceUuid, Pattern excludeNames)
            throws CloudFoundryException;

    void bindServiceInstance(ApplicationIdentity application, String serviceInstanceId)
            throws EntityNotFoundException, CloudFoundryException;

    void bindServiceInstance(List<ApplicationIdentity> application, String serviceInstanceId)
            throws EntityNotFoundException, CloudFoundryException;
}
