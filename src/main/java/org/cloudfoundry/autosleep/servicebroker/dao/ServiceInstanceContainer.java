package org.cloudfoundry.autosleep.servicebroker.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by buce8373 on 15/10/2015.
 */
@Data
@AllArgsConstructor
public class ServiceInstanceContainer {
    private ServiceInstance serviceInstance;

    private final Map<String, ServiceInstanceBinding> bindings = new HashMap<>();
}
