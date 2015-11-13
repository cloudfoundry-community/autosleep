package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.util.EqualUtil;
import org.cloudfoundry.autosleep.util.serializer.IntervalDeserializer;
import org.cloudfoundry.autosleep.util.serializer.IntervalSerializer;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;


@Getter
@Setter
@Slf4j
public class AutosleepServiceInstance extends org.cloudfoundry.community.servicebroker.model.ServiceInstance {
    public static final String INACTIVITY_PARAMETER = "inactivity";

    @JsonSerialize(using = IntervalSerializer.class)
    @JsonDeserialize(using = IntervalDeserializer.class)
    private Duration interval;

    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private AutosleepServiceInstance() {
        super(new CreateServiceInstanceRequest());
    }

    public AutosleepServiceInstance(CreateServiceInstanceRequest request) throws HttpMessageNotReadableException {
        super(request);
        setDurationFromParams(request.getParameters());
    }

    public AutosleepServiceInstance(UpdateServiceInstanceRequest request) throws HttpMessageNotReadableException,
            ServiceInstanceUpdateNotSupportedException {
        super(request);
        setDurationFromParams(request.getParameters());
    }

    public AutosleepServiceInstance(DeleteServiceInstanceRequest request) {
        super(request);
    }

    private void setDurationFromParams(Map<String, Object> params) throws HttpMessageNotReadableException {

        if (params == null || params.get(INACTIVITY_PARAMETER) == null) {
            interval = Config.defaultInactivityPeriod;
        } else {
            String inactivityPattern = (String) params.get(INACTIVITY_PARAMETER);
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


    @Override
    public String toString() {
        return "AutoSleepSI:[id:" + getServiceInstanceId() + " interval:+" + getInterval().toString() + " sdid:"
                + getServiceDefinitionId() + "dashURL:" + getDashboardUrl() + " org:" + getOrganizationGuid()
                + " plan:" + getPlanId() + " space:"
                + getSpaceGuid() + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof AutosleepServiceInstance)) {
            return false;
        }
        AutosleepServiceInstance other = (AutosleepServiceInstance) object;

        return EqualUtil.areEquals(this.getServiceInstanceId(), other.getServiceInstanceId())
                && EqualUtil.areEquals(this.getServiceDefinitionId(), other.getServiceDefinitionId())
                && EqualUtil.areEquals(this.getInterval(), other.getInterval())
                && EqualUtil.areEquals(this.getDashboardUrl(), other.getDashboardUrl())
                && EqualUtil.areEquals(this.getOrganizationGuid(), other.getOrganizationGuid())
                && EqualUtil.areEquals(this.getPlanId(), other.getPlanId())
                && EqualUtil.areEquals(this.getSpaceGuid(), other.getSpaceGuid());
    }

    @Override
    public int hashCode() {
        return this.getServiceInstanceId().hashCode();
    }

}
