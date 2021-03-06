package org.cloudfoundry.autosleep.access.dao.model;

import java.time.Duration;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.cloudfoundry.autosleep.config.Config.ServiceInstanceParameters;
import org.cloudfoundry.autosleep.config.EnrollmentConfig.EnrollmentParameters;
import org.cloudfoundry.autosleep.util.serializer.DurationDeserializer;
import org.cloudfoundry.autosleep.util.serializer.DurationSerializer;
import org.cloudfoundry.autosleep.util.serializer.PatternDeserializer;
import org.cloudfoundry.autosleep.util.serializer.PatternSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
@EqualsAndHashCode(of = {"organizationGuid"})
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgEnrollmentConfig {

    @Id
    @JsonProperty(access = Access.WRITE_ONLY)
    private String organizationGuid;

    @Lob
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    @JsonProperty ("idle-duration")
    private Duration idleDuration;

    @JsonSerialize(using = PatternSerializer.class)
    @JsonDeserialize(using = PatternDeserializer.class)
    @Lob
    @Column
    @JsonProperty("exclude-spaces-from-auto-enrollment")
    private Pattern excludeSpacesFromAutoEnrollment;

    @Enumerated(EnumType.ORDINAL)
    @JsonProperty("state")
    private EnrollmentParameters.EnrollmentState state;

    @Enumerated(EnumType.ORDINAL)
    @JsonProperty("auto-enrollment")
    private ServiceInstanceParameters.Enrollment autoEnrollment;

}
