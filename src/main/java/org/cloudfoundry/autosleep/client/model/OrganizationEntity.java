package org.cloudfoundry.autosleep.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationEntity extends  AbstractEntity{

    @JsonProperty("spaces_url")
    private String spacesUrl;

}
