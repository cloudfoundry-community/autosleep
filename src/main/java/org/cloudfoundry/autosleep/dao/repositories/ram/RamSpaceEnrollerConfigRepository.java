package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;


public class RamSpaceEnrollerConfigRepository extends HashmapRepository<SpaceEnrollerConfig> implements
        SpaceEnrollerConfigRepository {

    @Override
    protected String getObjectId(SpaceEnrollerConfig object) {
        return object.getId();
    }

}
