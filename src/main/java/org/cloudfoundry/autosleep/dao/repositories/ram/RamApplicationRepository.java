package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;


public class RamApplicationRepository extends HashmapRepository<ApplicationInfo> implements ApplicationRepository {

    @Override
    protected String getObjectId(ApplicationInfo app) {
        return app.getUuid().toString();
    }

}
