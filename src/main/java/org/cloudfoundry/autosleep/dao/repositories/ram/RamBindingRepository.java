package org.cloudfoundry.autosleep.dao.repositories.ram;

import org.cloudfoundry.autosleep.dao.model.ASServiceBinding;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;


public class RamBindingRepository extends HashmapRepository<ASServiceBinding> implements BindingRepository {

    @Override
    protected String getObjectId(ASServiceBinding binding) {
        return binding.getId();
    }

}
