package org.cloudfoundry.autosleep.dao.model;

import org.junit.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;



public class SpaceEnrollerConfigTest {
    private static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();

    private static final String SERVICE_DEFINITION_ID = UUID.randomUUID().toString();

    private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

    private static final String SPACE_ID = UUID.randomUUID().toString();

    private static final String PLAN_ID = UUID.randomUUID().toString();

    private SpaceEnrollerConfig getNewServiceInstance() {
        return SpaceEnrollerConfig.builder().id(SERVICE_INSTANCE_ID)
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .forcedAutoEnrollment(true)
                .idleDuration(Duration.ofHours(1))
                .planId(PLAN_ID)
                .organizationId(ORGANIZATION_ID)
                .spaceId(SPACE_ID)
                .excludeFromAutoEnrollment(Pattern.compile(".*"))
                .secret("secret").build();
    }

    @Test
    public void testEquals() {
        assertThat(getNewServiceInstance(), is(not(nullValue())));
        assertThat(getNewServiceInstance(), is(not(equalTo("toto"))));
        assertThat(getNewServiceInstance(), is(equalTo(getNewServiceInstance())));
    }

    @Test
    public void testHashCode() {
        assertThat(getNewServiceInstance().hashCode(), is(equalTo(getNewServiceInstance().hashCode())));
    }

    @Test
    public void testToString() {
        assertThat(getNewServiceInstance().toString(), is(not(nullValue())));
    }


}