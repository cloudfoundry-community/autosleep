package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Deployment {

    @JsonProperty("application_id")
    private UUID applicationId;

    @JsonProperty("application_name")
    private String applicationName;

    @JsonProperty("application_uris")
    private List<String> applicationUris;

    public String getFirstUri() {
        if (applicationUris == null || applicationUris.size() == 0) {
            return null;
        } else {
            return applicationUris.get(0);
        }
    }


}
