package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.util.EqualUtil;
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

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        updateParams(request.getParameters());
    }

    public AutosleepServiceInstance(UpdateServiceInstanceRequest request) throws HttpMessageNotReadableException,
            ServiceInstanceUpdateNotSupportedException {
        super(request);
        updateParams(request.getParameters());

    }

    public AutosleepServiceInstance(DeleteServiceInstanceRequest request) {
        super(request);
    }

    public void updateFromRequest(UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException {
        if (!getPlanId().equals(request.getPlanId())) {
            /* org.cloudfoundry.community.servicebroker.model.ServiceInstance doesn't let us modify planId field
             * (private), and only handle service instance updates by re-creating them from scratch. As we need to
             * handle real updates (secret params), we are not supporting plan updates for now.*/
            throw new ServiceInstanceUpdateNotSupportedException("Service plan updates not supported.");
        }
        updateParams(request.getParameters());
    }

    private void updateParams(Map<String, Object> params) {
        setDurationFromParams(params);
        setIgnoreNamesFromParams(params);
        setNoOptOutFromParams(params);
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

    private void setIgnoreNamesFromParams(Map<String, Object> params) throws HttpMessageNotReadableException {
        if (params != null && params.get(EXCLUDE_PARAMETER) != null) {
            String excludeNames = (String) params.get(EXCLUDE_PARAMETER);
            if (!excludeNames.trim().equals("")) {
                log.debug("excludeNames " + excludeNames);
                try {
                    this.excludeNames = Pattern.compile(excludeNames);
                } catch (PatternSyntaxException p) {
                    log.error("Wrong format for exclusion  - format cannot be compiled to a valid regexp");
                    throw new HttpMessageNotReadableException("'" + EXCLUDE_PARAMETER + "' should be a valid regexp");
                }
            }
        }
    }

    private void setNoOptOutFromParams(Map<String, Object> params) throws HttpMessageNotReadableException {
        if (params != null) {
            String noOptParam = (String) params.get(NO_OPTOUT_PARAMETER);
            if (noOptParam != null) {
                if (isAuthorized(params)) {
                    this.noOptOut = Boolean.parseBoolean(noOptParam);
                } else {
                    throwUnauthorizedException(NO_OPTOUT_PARAMETER);
                }
            }
        }
    }


    /**
     * Return true if the request is authorized to modify 'protected' parameters.
     *
     * @param params request parameters
     * @return true if a the secret parameter is present, and if its value is the same as in previous calls (if it
     * was previously received)
     */
    private boolean isAuthorized(Map<String, Object> params) {
        boolean isAuthorized = false;
        if (params != null && params.get(SECRET_PARAMETER) != null) {
            String receivedSecret = Base64.getUrlEncoder().encodeToString(((String) params.get(SECRET_PARAMETER))
                    .getBytes(Charset.forName("UTF-8")));
            isAuthorized = getSecretHash() == null || getSecretHash().equals(receivedSecret);
            if (isAuthorized) {
                setSecretHash(receivedSecret);
            }
        }
        return isAuthorized;
    }

    private void throwUnauthorizedException(String parameterName) {
        throw new HttpMessageNotReadableException("Trying to set or change a protected parameter ('"
                + parameterName + "') without providing the right '" + SECRET_PARAMETER + "'.");
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

        return EqualUtil.areEquals(this.getServiceInstanceId(), other.getServiceInstanceId())
                && EqualUtil.areEquals(this.getServiceDefinitionId(), other.getServiceDefinitionId())
                && EqualUtil.areEquals(this.getInterval(), other.getInterval())
                && EqualUtil.areEquals(this.isNoOptOut(), other.isNoOptOut())
                && EqualUtil.areEquals(this.getDashboardUrl(), other.getDashboardUrl())
                && EqualUtil.areEquals(this.getOrganizationGuid(), other.getOrganizationGuid())
                && EqualUtil.areEquals(this.getPlanId(), other.getPlanId())
                && EqualUtil.areEquals(this.getSpaceGuid(), other.getSpaceGuid())
                && EqualUtil.areEquals(this.getSecretHash(), other.getSecretHash())
                //Pattern does not implement equals
                && EqualUtil.areEquals(this.getExcludeNames() == null ? null : this.getExcludeNames().pattern(),
                other.getExcludeNames() == null ? null : other.getExcludeNames().pattern());
    }

    @Override
    public int hashCode() {
        return this.getServiceInstanceId().hashCode();
    }

}
