package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Deployment {
    @JsonProperty("application_id")
    private UUID applicationId;

    @JsonProperty("application_name")
    private String applicationName;


}
