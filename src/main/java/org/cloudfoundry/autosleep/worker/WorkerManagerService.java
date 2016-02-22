package org.cloudfoundry.autosleep.worker;

import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;

public interface WorkerManagerService {

    void registerApplicationStopper(SpaceEnrollerConfig config, String applicationId, String appBindingId);

    void registerSpaceEnroller(SpaceEnrollerConfig config);

}
