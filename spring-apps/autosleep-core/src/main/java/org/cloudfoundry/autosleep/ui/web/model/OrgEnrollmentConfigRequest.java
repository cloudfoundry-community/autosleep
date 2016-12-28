package org.cloudfoundry.autosleep.ui.web.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(of = { "organizationGuid" })
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgEnrollmentConfigRequest {

    @Id
    @JsonProperty(access = Access.WRITE_ONLY)
    private String organizationGuid;

    @JsonProperty("idle-duration")
    private String idleDuration;

    @JsonProperty("exclude-spaces-from-auto-enrollment")
    private String excludeSpacesFromAutoEnrollment;

    @JsonProperty("state")
    private String state;

    @JsonProperty("auto-enrollment")
    private String autoEnrollment;

}
