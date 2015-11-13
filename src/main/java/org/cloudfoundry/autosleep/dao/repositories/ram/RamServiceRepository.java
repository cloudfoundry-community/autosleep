package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;


public class RamServiceRepository  extends HashmapRepository<AutosleepServiceInstance> implements ServiceRepository {

    @Override
    protected String getObjectId(AutosleepServiceInstance object) {
        return object.getServiceInstanceId();
    }

}
