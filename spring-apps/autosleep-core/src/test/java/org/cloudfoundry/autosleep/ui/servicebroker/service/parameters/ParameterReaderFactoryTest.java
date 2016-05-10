/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.ui.servicebroker.service.parameters;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.Config.ServiceInstanceParameters;
import org.cloudfoundry.autosleep.config.Config.ServiceInstanceParameters.Enrollment;
import org.cloudfoundry.autosleep.ui.servicebroker.service.InvalidParameterException;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReaderFactory;
import org.junit.Test;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ParameterReaderFactoryTest {

    private ParameterReaderFactory factory = new ParameterReaderFactory();

    @Test
    public void test_auto_enrollment_fails_to_read_bad_syntax() {
        //Given the parameter reader for auto enrollment
        ParameterReader<Enrollment> enrollmentParameterReader = factory.buildAutoEnrollmentReader();
        //When we submit a bad syntax
        //Then it fails
        verifyThrown(() -> enrollmentParameterReader
                        .readParameter("fart", true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(ServiceInstanceParameters.AUTO_ENROLLMENT))));
    }

    @Test
    public void test_auto_enrollment_handle_right_parameter() {
        //Given the parameter reader for auto enrollment
        ParameterReader<Enrollment> enrollmentParameterReader = factory.buildAutoEnrollmentReader();
        //When we ask the parameter
        String parameter = enrollmentParameterReader.getParameterName();
        //Then we obtain the auto enrollment parameter
        assertThat(parameter, is(equalTo(ServiceInstanceParameters.AUTO_ENROLLMENT)));
    }

    @Test
    public void test_auto_enrollment_read_parameter() {
        //Given the parameter reader for auto enrollment
        ParameterReader<Enrollment> enrollmentParameterReader = factory.buildAutoEnrollmentReader();
        //When we read a good syntax
        Enrollment standard = enrollmentParameterReader
                .readParameter(Enrollment.standard.name(), true);
        Enrollment forced = enrollmentParameterReader
                .readParameter(Enrollment.forced.name(), false);
        //Then we obtained the values from enum
        assertThat(standard, is(equalTo(Enrollment.standard)));
        assertThat(forced, is(equalTo(Enrollment.forced)));

    }

    @Test
    public void test_auto_enrollment_returns_default_when_null_submitted_with_default() {
        //Given the parameter reader for auto enrollment
        ParameterReader<Enrollment> enrollmentParameterReader = factory.buildAutoEnrollmentReader();
        //When we ask to read null with default
        Enrollment withDefault = enrollmentParameterReader.readParameter(null, true);
        //Then it returns the value from config
        assertThat(withDefault, is(equalTo(Enrollment.standard)));
    }

    @Test
    public void test_auto_enrollment_returns_null_when_null_submitted_without_default() {
        //Given the parameter reader for auto enrollment
        ParameterReader<Enrollment> enrollmentParameterReader = factory.buildAutoEnrollmentReader();
        //When we ask to read null with default
        Enrollment withoutDefault = enrollmentParameterReader.readParameter(null, false);
        //Then it returns null
        assertThat(withoutDefault, is(nullValue()));
    }

    @Test
    public void test_exclude_fails_to_read_bad_syntax() {
        //Given the parameter reader for exclude from auto enrollment
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        //When we submit a bad syntax
        //Then it fails
        verifyThrown(() -> excludeFromAutoEnrollmentReader
                        .readParameter("*",
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT))));
    }

    @Test
    public void test_exclude_handle_right_parameter() {
        //Given the parameter reader for exclude from auto enrollment
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        //When we ask the parameter
        String parameter = excludeFromAutoEnrollmentReader.getParameterName();
        //Then we obtain the exclude from auto enrollment parameter
        assertThat(parameter, is(equalTo(ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)));
    }

    @Test
    public void test_exclude_read_parameter() {
        //Given the parameter reader for exclude from auto enrollment
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        //When we read a good syntax
        String pattern = ".*";
        Pattern result = excludeFromAutoEnrollmentReader.readParameter(pattern, true);
        //Then we obtained the right compiled pattern
        assertThat(result, is(not(nullValue())));
        assertThat(result.pattern(), is(equalTo(pattern)));
    }

    @Test
    public void test_exclude_returns_null_when_null_submitted_with_default() {
        //Given the parameter reader for exclude from auto enrollment
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        //When we ask to read null with default
        Pattern withDefault = excludeFromAutoEnrollmentReader.readParameter(null, true);
        //Then it returns null
        assertThat(withDefault, is(nullValue()));
    }

    @Test
    public void test_exclude_returns_null_when_null_submitted_without_default() {
        //Given the parameter reader for exclude from auto enrollment
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        //When we ask to read null with default
        Pattern withoutDefault = excludeFromAutoEnrollmentReader.readParameter(null, true);
        //Then it returns null
        assertThat(withoutDefault, is(nullValue()));
    }

    @Test
    public void test_exclude_trim_submitted_values() {
        //Given the parameter reader for exclude from auto enrollment
        ParameterReader<Pattern> excludeFromAutoEnrollmentReader = factory
                .buildExcludeFromAutoEnrollmentReader();
        //When we ask to read a value full of spaces with default
        Pattern withSpaces = excludeFromAutoEnrollmentReader.readParameter("     ", true);
        //Then it returns null
        assertThat(withSpaces, is(nullValue()));
    }

    @Test
    public void test_idle_duration_fails_to_read_bad_syntax() {
        //Given the parameter reader for idle duration
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        //When we submit a bad syntax
        //Then it fails
        verifyThrown(() -> idleDurationReader
                        .readParameter("12",
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(ServiceInstanceParameters.IDLE_DURATION))));
    }

    @Test
    public void test_idle_duration_handle_right_parameter() {
        //Given the parameter reader for idle duration
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        //When we ask the parameter
        String parameter = idleDurationReader.getParameterName();
        //Then we obtain the duration parameter
        assertThat(parameter, is(equalTo(ServiceInstanceParameters.IDLE_DURATION)));
    }

    @Test
    public void test_idle_duration_read_parameter() {
        //Given the parameter reader for idle duration
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        //When we read a good parameter
        Duration duration = idleDurationReader.readParameter("PT12M", true);
        //Then the value returned is ok
        assertThat(duration, is(equalTo(Duration.ofMinutes(12))));
    }

    @Test
    public void test_idle_duration_returns_default_when_null_submitted_with_default() {
        //Given the parameter reader for idle duration
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        //When we ask to read null with default
        Duration withDefault = idleDurationReader.readParameter(null, true);
        //Then it returns the value from config
        assertThat(withDefault, is(equalTo(Config.DEFAULT_INACTIVITY_PERIOD)));
    }

    @Test
    public void test_idle_duration_returns_null_when_null_submitted_without_default() {
        //Given the parameter reader for idle duration
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        //When we ask to read null without  default
        Duration withoutDefault = idleDurationReader.readParameter(null, false);
        //Then it returns null
        assertThat(withoutDefault, is(nullValue()));
    }

    @Test
    public void test_ignore_route_error_fails_to_read_bad_syntax() {
        //Given the parameter reader for ignore route error
        ParameterReader<Boolean> ignoreRouteServiceErrorReader = factory.buildIgnoreRouteServiceErrorReader();
        //When we submit a bad syntax
        //Then it fails
        verifyThrown(() -> ignoreRouteServiceErrorReader
                        .readParameter("toto",
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(ServiceInstanceParameters.IGNORE_ROUTE_SERVICE_ERROR))));
    }

    @Test
    public void test_ignore_route_error_handle_right_parameter() {
        //Given the parameter reader for ignore route error
        ParameterReader<Boolean> ignoreRouteServiceErrorReader = factory.buildIgnoreRouteServiceErrorReader();
        //When we ask the parameter
        String parameter = ignoreRouteServiceErrorReader.getParameterName();
        //Then we obtain the auto enrollment parameter
        assertThat(parameter, is(equalTo(ServiceInstanceParameters.IGNORE_ROUTE_SERVICE_ERROR)));
    }

    @Test
    public void test_ignore_route_error_read_parameter() {
        //Given the parameter reader for ignore route error
        ParameterReader<Boolean> ignoreRouteServiceErrorReader = factory.buildIgnoreRouteServiceErrorReader();
        //When we read a bad syntax
        Boolean ignoreRouteError = ignoreRouteServiceErrorReader.readParameter(Boolean.TRUE.toString(), true);
        //Then we obtained the value submitted
        assertTrue(ignoreRouteError);
    }

    @Test
    public void test_ignore_route_error_returns_default_when_null_submitted_with_default() {
        //Given the parameter reader for ignore route error
        ParameterReader<Boolean> ignoreRouteServiceErrorReader = factory.buildIgnoreRouteServiceErrorReader();
        //When we ask to read null with default
        Boolean withDefault = ignoreRouteServiceErrorReader.readParameter(null, true);
        //Then it returns default
        assertThat(withDefault, is(equalTo(Config.DEFAULT_IGNORE_SERVICE_ERROR)));
    }

    //

    @Test
    public void test_ignore_route_error_returns_default_when_null_submitted_without_default() {
        //Given the parameter reader for ignore route error
        ParameterReader<Boolean> ignoreRouteServiceErrorReader = factory.buildIgnoreRouteServiceErrorReader();
        //When we ask to read null with default
        Boolean withoutDefault = ignoreRouteServiceErrorReader.readParameter(null, false);
        //Then it returns default
        assertThat(withoutDefault, is(equalTo(Config.DEFAULT_IGNORE_SERVICE_ERROR)));
    }

    @Test
    public void test_secret_handle_right_parameter() {
        //Given the parameter reader for secret
        ParameterReader<String> secretReader = factory.buildSecretReader();
        //When we ask the parameter
        String parameter = secretReader.getParameterName();
        //Then we obtain the auto enrollment parameter
        assertThat(parameter, is(equalTo(ServiceInstanceParameters.SECRET)));
    }

    @Test
    public void test_secret_read_parameter() {
        //Given the parameter reader for secret
        ParameterReader<String> secretReader = factory.buildSecretReader();
        //When we read a good syntax
        String secret = "P@ssword!";
        String readSecret = secretReader.readParameter(secret, true);
        //Then we obtained the value submitted
        assertThat(readSecret, is(equalTo(secret)));
    }

    @Test
    public void test_secret_returns_null_when_null_submitted_with_default() {
        //Given the parameter reader for secret
        ParameterReader<String> secretReader = factory.buildSecretReader();
        //When we ask to read null with default
        String withDefault = secretReader.readParameter(null, true);
        //Then it returns null
        assertThat(withDefault, is(nullValue()));
    }

    @Test
    public void test_secret_returns_null_when_null_submitted_without_default() {
        //Given the parameter reader for secret
        ParameterReader<String> secretReader = factory.buildSecretReader();
        //When we ask to read null with default
        String withoutDefault = secretReader.readParameter(null, true);
        //Then it returns null
        assertThat(withoutDefault, is(nullValue()));
    }

}