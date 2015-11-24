package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Deployment {

    @JsonProperty("application_id")
    @Getter
    @Setter
    private UUID applicationId;

    @JsonProperty("application_name")
    @Getter
    @Setter
    private String applicationName;

    @JsonProperty("application_uris")
    private String[] applicationUris;

    public String getFirstUri() {
        return applicationUris[0];
    }

    public void setApplicationUris(String[] uris) {
        this.applicationUris = uris.clone();
    }


}
