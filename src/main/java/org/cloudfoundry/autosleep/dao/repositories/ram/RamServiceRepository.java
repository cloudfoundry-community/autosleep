package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;


public class RamServiceRepository  extends HashmapRepository<SpaceEnrollerConfig> implements ServiceRepository {

    @Override
    protected String getObjectId(SpaceEnrollerConfig object) {
        return object.getServiceInstanceId();
    }

}
