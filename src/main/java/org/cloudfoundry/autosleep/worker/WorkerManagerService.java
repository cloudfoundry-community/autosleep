package org.cloudfoundry.autosleep.worker;

import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;

/**
 * Created by BUCE8373 on 23/12/2015.
 */
public interface WorkerManagerService {

    void registerApplicationStopper(SpaceEnrollerConfig config, String applicationId);

    void registerSpaceEnroller(SpaceEnrollerConfig config);
}
