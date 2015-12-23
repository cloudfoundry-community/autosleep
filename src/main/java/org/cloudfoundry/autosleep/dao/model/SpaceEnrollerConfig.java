package org.cloudfoundry.autosleep.dao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.util.serializer.IntervalDeserializer;
import org.cloudfoundry.autosleep.util.serializer.IntervalSerializer;
import org.cloudfoundry.autosleep.util.serializer.PatternDeserializer;
import org.cloudfoundry.autosleep.util.serializer.PatternSerializer;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Setter
@Slf4j
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Entity
public class SpaceEnrollerConfig {

    @Id
    @JsonProperty
    private String id;

    @JsonProperty
    private String serviceDefinitionId;

    @JsonProperty
    private String planId;

    @JsonProperty
    private String organizationId;

    @JsonProperty
    private String spaceId;

    @JsonSerialize(using = IntervalSerializer.class)
    @JsonDeserialize(using = IntervalDeserializer.class)
    private Duration idleDuration;

    @JsonSerialize(using = PatternSerializer.class)
    @JsonDeserialize(using = PatternDeserializer.class)
    private Pattern excludeFromAutoEnrollment;

    @JsonProperty
    private boolean forcedAutoEnrollment;

    @JsonProperty
    private String secret;


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : [id:" + getId()
                + " idleDuration:+" + idleDuration.toString()
                + " excludeFromAutoEnrollment:"
                + (excludeFromAutoEnrollment != null ? excludeFromAutoEnrollment.toString() : "")
                + " forcedAutoEnrollment:" + forcedAutoEnrollment
                + " space:" + spaceId + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof SpaceEnrollerConfig)) {
            return false;
        } else {
            SpaceEnrollerConfig other = SpaceEnrollerConfig.class.cast(object);
            return Objects.equals(this.getId(), other.getId());
        }
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

}
