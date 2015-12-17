package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;


public class RamBindingRepository extends HashmapRepository<ApplicationBinding> implements BindingRepository {

    @Override
    protected String getObjectId(ApplicationBinding binding) {
        return binding.getServiceBindingId();
    }

}
