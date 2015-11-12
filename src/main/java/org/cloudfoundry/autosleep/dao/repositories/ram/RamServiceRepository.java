package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.ASServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;


public class RamServiceRepository  extends HashmapRepository<ASServiceInstance> implements ServiceRepository {

    @Override
    protected String getObjectId(ASServiceInstance object) {
        return object.getServiceInstanceId();
    }

}
