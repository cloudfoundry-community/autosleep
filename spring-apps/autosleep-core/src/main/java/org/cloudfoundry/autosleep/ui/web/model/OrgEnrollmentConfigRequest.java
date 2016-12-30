package org.cloudfoundry.autosleep.ui.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrgEnrollmentConfigRequest {

    @JsonProperty("idle-duration")
    private String idleDuration;

    @JsonProperty("exclude-spaces-from-auto-enrollment")
    private String excludeSpacesFromAutoEnrollment;

    @JsonProperty("state")
    private String state;

    @JsonProperty("auto-enrollment")
    private String autoEnrollment;

}
