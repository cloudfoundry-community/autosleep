package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.util.serializer.IntervalDeserializer;
import org.cloudfoundry.autosleep.util.serializer.IntervalSerializer;
import org.cloudfoundry.autosleep.util.serializer.PatternDeserializer;
import org.cloudfoundry.autosleep.util.serializer.PatternSerializer;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Setter
@Slf4j
public class AutosleepServiceInstance extends ServiceInstance {
    public static final String INACTIVITY_PARAMETER = "inactivity";

    public static final String EXCLUDE_PARAMETER = "excludeAppNameRegExp";

    public static final String NO_OPTOUT_PARAMETER = "no_optout";

    public static final String SECRET_PARAMETER = "secret";

    @JsonSerialize(using = IntervalSerializer.class)
    @JsonDeserialize(using = IntervalDeserializer.class)
    private Duration interval;

    @JsonSerialize(using = PatternSerializer.class)
    @JsonDeserialize(using = PatternDeserializer.class)
    private Pattern excludeNames;

    private boolean noOptOut;

    @JsonProperty
    private String secretHash;

    /**
     * Should never be called. Only for JSON auto serialization.
     */
    @SuppressWarnings("unused")
    private AutosleepServiceInstance() {
        super(new CreateServiceInstanceRequest());
    }

    public AutosleepServiceInstance(CreateServiceInstanceRequest request) throws HttpMessageNotReadableException {
        super(request);
        interval = Config.DEFAULT_INACTIVITY_PERIOD;
        noOptOut = Boolean.FALSE;
        updateFromParameters(request.getParameters());
    }

    public AutosleepServiceInstance(UpdateServiceInstanceRequest request) throws HttpMessageNotReadableException,
            ServiceInstanceUpdateNotSupportedException {
        super(request);
        updateFromParameters(request.getParameters());

    }

    public AutosleepServiceInstance(DeleteServiceInstanceRequest request) {
        super(request);
    }

    public void updateFromParameters(Map<String, Object> params) {
        if (params.containsKey(SECRET_PARAMETER)) {
            secretHash = (String) params.get(SECRET_PARAMETER);
        }
        if (params.containsKey(INACTIVITY_PARAMETER)) {
            interval = (Duration) params.get(INACTIVITY_PARAMETER);
        }
        if (params.containsKey(EXCLUDE_PARAMETER)) {
            excludeNames = (Pattern) params.get(EXCLUDE_PARAMETER);
        }
        if (params.containsKey(NO_OPTOUT_PARAMETER)) {
            noOptOut = (Boolean) params.get(NO_OPTOUT_PARAMETER);
        }
    }

    @Override
    public String toString() {
        return "AutoSleepSI:[id:" + getServiceInstanceId() + " interval:+" + getInterval().toString()
                + " excludesNames:" + (getExcludeNames() != null ? getExcludeNames().toString() : "")
                + " noOptOut:" + isNoOptOut()
                + " sdid:" + getServiceDefinitionId()
                + " dashURL:" + getDashboardUrl()
                + " org:" + getOrganizationGuid()
                + " plan:" + getPlanId()
                + " space:" + getSpaceGuid() + "]";
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

        return Objects.equals(this.getServiceInstanceId(), other.getServiceInstanceId())
                && Objects.equals(this.getServiceDefinitionId(), other.getServiceDefinitionId())
                && Objects.equals(this.getInterval(), other.getInterval())
                && Objects.equals(this.isNoOptOut(), other.isNoOptOut())
                && Objects.equals(this.getDashboardUrl(), other.getDashboardUrl())
                && Objects.equals(this.getOrganizationGuid(), other.getOrganizationGuid())
                && Objects.equals(this.getPlanId(), other.getPlanId())
                && Objects.equals(this.getSpaceGuid(), other.getSpaceGuid())
                && Objects.equals(this.getSecretHash(), other.getSecretHash())
                //Pattern does not implement equals
                && Objects.equals(this.getExcludeNames() == null ? null : this.getExcludeNames().pattern(),
                other.getExcludeNames() == null ? null : other.getExcludeNames().pattern());
    }

    @Override
    public int hashCode() {
        return this.getServiceInstanceId().hashCode();
    }

}
