package org.cloudfoundry.autosleep.servicebroker.service.parameters;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.servicebroker.service.InvalidParameterException;
import org.junit.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import static org.hamcrest.Matchers.is;


import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;

public class ParameterReaderFactoryTest {

    private ParameterReaderFactory factory = new ParameterReaderFactory();

    @Test
    public void testBuildIdleDurationReader() {
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        assertThat(idleDurationReader.getParameterName(), is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION)));

        //default case
        assertThat(idleDurationReader.readParameter(Collections.emptyMap(), true),
                is(equalTo(Config.DEFAULT_INACTIVITY_PERIOD)));
        assertThat(idleDurationReader.readParameter(Collections.emptyMap(), false),
                is(nullValue()));

        //bad syntax
        verifyThrown(() -> idleDurationReader
                        .readParameter(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "12"),
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION))));

        //good syntax
        assertThat(idleDurationReader
                .readParameter(Collections.singletonMap(Config.ServiceInstanceParameters.IDLE_DURATION, "PT12M"),
                        true), is(equalTo(Duration.ofMinutes(12))));

    }


    @Test
    public void testBuildAutoEnrollmentReader() {
        ParameterReader<Config.ServiceInstanceParameters.Enrollment> enrollmentParameterReader = factory
                .buildAutoEnrollmentReader();
        assertThat(enrollmentParameterReader.getParameterName(),
                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT)));
        //default case are null
        assertThat(enrollmentParameterReader.readParameter(Collections.emptyMap(), true),
                is(Config.ServiceInstanceParameters.Enrollment.standard));
        assertThat(enrollmentParameterReader.readParameter(Collections.emptyMap(), false),
                is(nullValue()));

        //bad syntax is well thrown
        verifyThrown(() -> enrollmentParameterReader
                        .readParameter(Collections.singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                                "fart"), true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));

        //good syntax
        Config.ServiceInstanceParameters.Enrollment enrollment = enrollmentParameterReader
                .readParameter(Collections.singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, Config
                        .ServiceInstanceParameters.Enrollment.standard.name()), true);
        assertThat(enrollment, is(equalTo(Config
                .ServiceInstanceParameters.Enrollment.standard)));
        enrollment = enrollmentParameterReader
                .readParameter(Collections.singletonMap(Config.ServiceInstanceParameters.AUTO_ENROLLMENT, Config
                        .ServiceInstanceParameters.Enrollment.forced.name()), false);
        assertThat(enrollment, is(equalTo(Config
                .ServiceInstanceParameters.Enrollment.forced)));
    }

    @Test
    public void testBuildExcludeFromAutoEnrollmentReader() {
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        assertThat(excludeFromAutoEnrollmentReader.getParameterName(),
                is(equalTo(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)));

        //default case are null
        assertThat(excludeFromAutoEnrollmentReader.readParameter(Collections.emptyMap(), false),
                is(nullValue()));
        assertThat(excludeFromAutoEnrollmentReader.readParameter(Collections.singletonMap(Config
                        .ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT, ""), false),
                is(nullValue()));
        assertThat(excludeFromAutoEnrollmentReader.readParameter(Collections.emptyMap(), false),
                is(nullValue()));

        //bad syntax is well thrown
        verifyThrown(() -> excludeFromAutoEnrollmentReader
                        .readParameter(Collections
                                        .singletonMap(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT,
                                                "*"),
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT))));

        String pattern = ".*";
        Pattern result = excludeFromAutoEnrollmentReader
                .readParameter(Collections.singletonMap(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT,
                        pattern), true);
        assertThat(result, is(not(nullValue())));
        assertThat(result.pattern(), is(equalTo(pattern)));
    }

    @Test
    public void testBuildSecretReader() {
        ParameterReader<String> secretReader = factory.buildSecretReader();
        assertThat(secretReader.getParameterName(), is(equalTo(Config.ServiceInstanceParameters.SECRET)));

        //default case are null
        assertThat(secretReader.readParameter(Collections.emptyMap(), false), is(nullValue()));
        assertThat(secretReader.readParameter(Collections.emptyMap(), false), is(nullValue()));

        String secret = "P@ssword!";
        assertThat(secretReader.readParameter(
                        Collections.singletonMap(Config.ServiceInstanceParameters.SECRET, secret), true),
                is(equalTo(secret)));
    }
}