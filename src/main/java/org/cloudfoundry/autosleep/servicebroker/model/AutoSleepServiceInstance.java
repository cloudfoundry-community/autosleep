package org.cloudfoundry.autosleep.servicebroker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;


@Getter
@Setter
@Slf4j
public class AutoSleepServiceInstance extends ServiceInstance {

    @JsonProperty("interval")
    @JsonSerialize(using = IntervalSerializer.class)
    @JsonDeserialize(using = IntervalDeserializer.class)
    private Duration interval;

    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private AutoSleepServiceInstance() {
        super(new CreateServiceInstanceRequest());
    }

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

    private static class IntervalSerializer extends JsonSerializer<Duration> {
        @Override
        public void serialize(Duration value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeNumber(value.toMillis());
        }
    }

    private static class IntervalDeserializer extends JsonDeserializer<Duration> {

        @Override
        public Duration deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
            return Duration.ofMillis(parser.getLongValue());
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
        if (!(object instanceof AutoSleepServiceInstance)) {
            return false;
        }
        AutoSleepServiceInstance other = (AutoSleepServiceInstance) object;

        return !(
                this.getServiceInstanceId() == null ? other.getServiceInstanceId() != null : !this
                        .getServiceInstanceId().equals(other.getServiceInstanceId())) && !(
                this.getInterval() == null ? other
                        .getInterval() != null : !this.getInterval().equals(other.getInterval())) && !(
                this.getDashboardUrl()
                        == null ? other.getDashboardUrl() != null : !this.getDashboardUrl().equals(other
                        .getDashboardUrl()))
                && !(
                this.getOrganizationGuid() == null ? other.getOrganizationGuid() != null : !this
                        .getOrganizationGuid().equals(other.getOrganizationGuid())) && !(
                this.getPlanId() == null ? other
                        .getPlanId() != null : !this.getPlanId().equals(other.getPlanId())) && !(
                this.getServiceDefinitionId()
                        == null ? other.getServiceDefinitionId() != null : !this.getServiceDefinitionId().equals(other
                        .getServiceDefinitionId()));
    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = 1;
        result = result * prime + getServiceInstanceId().hashCode();
        result = result * prime + getInterval().hashCode();
        result = result * prime + getSpaceGuid().hashCode();
        result = result * prime + getPlanId().hashCode();
        result = result * prime + getDashboardUrl().hashCode();
        result = result * prime + getOrganizationGuid().hashCode();
        result = result * prime + getServiceDefinitionId().hashCode();
        return result;
    }
}
