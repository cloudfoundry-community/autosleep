package org.cloudfoundry.autosleep.servicebroker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Data()
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AutoSleepServiceInstance extends ServiceInstance {
    private Duration interval;

    public AutoSleepServiceInstance(CreateServiceInstanceRequest request) throws HttpMessageNotReadableException {
        super(request);
        setDurationFromParams(request.getParameters());
    }

    public AutoSleepServiceInstance(UpdateServiceInstanceRequest request) throws HttpMessageNotReadableException,
            ServiceInstanceUpdateNotSupportedException {
        super(request);
        setDurationFromParams(request.getParameters());
        //TODO: support it when pull request accepted: https://github
        // .com/cloudfoundry-community/spring-boot-cf-service-broker/pull/35
        throw new ServiceInstanceUpdateNotSupportedException("update not supported");
    }

    public AutoSleepServiceInstance(DeleteServiceInstanceRequest request) {
        super(request);
    }

    private void setDurationFromParams(Map<String, Object> params) throws HttpMessageNotReadableException {

        if (params == null || params.get("inactivity") == null) {
            interval = Config.defaultInactivityPeriod;
        } else {
            String inactivityPattern = (String) params.get("inactivity");
            log.debug("pattern " + inactivityPattern);
            try {
                interval = Duration.parse(inactivityPattern);
            } catch (DateTimeParseException e) {
                log.error("Wrong format for inactivity duration - format should respect ISO-8601 duration format "
                        + "PnDTnHnMn");
                throw new HttpMessageNotReadableException("'inactivity' param badly formatted (ISO-8601). "
                        + "Example: \"PT15M\" for 15mn");
            }
        }
    }
}
