package org.cloudfoundry.autosleep.util;

import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;

import java.util.Collections;
import java.util.UUID;

public class BeanGenerator {
    public static final UUID ORG_TEST = UUID.randomUUID();
    public static final UUID SPACE_TEST = UUID.randomUUID();
    public static final UUID SERVICE_DEFINITION_ID = UUID.randomUUID();
    public static final UUID PLAN_ID = UUID.randomUUID();

    private static final CreateServiceInstanceRequest createRequest =
            new CreateServiceInstanceRequest(SERVICE_DEFINITION_ID.toString(),
                    PLAN_ID.toString(),
                    ORG_TEST.toString(),
                    SPACE_TEST.toString(),
                    Collections.emptyMap());

    public static AutosleepServiceInstance createServiceInstance() {
        return createServiceInstance(null);
    }

    public static AutosleepServiceInstance createServiceInstance(String serviceId) {
        if (serviceId == null) {
            serviceId = UUID.randomUUID().toString();
        }
        return new AutosleepServiceInstance(createRequest.withServiceInstanceId(serviceId));
    }

    public static ApplicationBinding createBinding(String serviceId, String bindingId, String appId) {
        if (serviceId == null) {
            serviceId = UUID.randomUUID().toString();
        }
        if (bindingId == null) {
            bindingId = UUID.randomUUID().toString();
        }
        if (appId == null) {
            appId = UUID.randomUUID().toString();
        }
        return new ApplicationBinding(bindingId, serviceId, null, null, appId);
    }

    public static ApplicationBinding createBinding(String bindingId) {
        return createBinding(null, bindingId, null);
    }

    public static ApplicationBinding createBinding() {
        return createBinding(null, null, null);
    }
}
