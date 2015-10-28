package org.cloudfoundry.autosleep.servicebroker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;

import java.util.Map;

@Data()
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AutoSleepServiceBinding extends ServiceInstanceBinding {
    public AutoSleepServiceBinding(String id, String serviceInstanceId, Map<String, Object> credentials, String
            syslogDrainUrl, String appGuid) {
        super(id, serviceInstanceId, credentials, syslogDrainUrl, appGuid);
    }
}
