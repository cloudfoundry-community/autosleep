package org.cloudfoundry.autosleep.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by buce8373 on 15/10/2015.
 */
@Data
@AllArgsConstructor
public class ServiceInstanceContainer {

    private ServiceInstance serviceInstance;
    private Duration interval;

    private final Map<String, ServiceInstanceBinding> bindings = new HashMap<>();
}
