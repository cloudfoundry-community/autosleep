/**
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
import org.cloudfoundry.autosleep.ui.servicebroker.service.InvalidParameterException;
import org.junit.Test;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.cloudfoundry.autosleep.util.TestUtils.verifyThrown;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ParameterReaderFactoryTest {

    private ParameterReaderFactory factory = new ParameterReaderFactory();

    @Test
    public void testBuildIdleDurationReader() {
        ParameterReader<Duration> idleDurationReader = factory.buildIdleDurationReader();
        assertThat(idleDurationReader.getParameterName(), is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION)));

        //default case
        assertThat(idleDurationReader.readParameter(null, true),
                is(equalTo(Config.DEFAULT_INACTIVITY_PERIOD)));
        assertThat(idleDurationReader.readParameter(null, false),
                is(nullValue()));

        //bad syntax
        verifyThrown(() -> idleDurationReader
                        .readParameter("12",
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.IDLE_DURATION))));

        //good syntax
        assertThat(idleDurationReader.readParameter("PT12M", true), is(equalTo(Duration.ofMinutes(12))));

    }


    @Test
    public void testBuildAutoEnrollmentReader() {
        ParameterReader<Config.ServiceInstanceParameters.Enrollment> enrollmentParameterReader = factory
                .buildAutoEnrollmentReader();
        assertThat(enrollmentParameterReader.getParameterName(),
                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT)));
        //default case are null
        assertThat(enrollmentParameterReader.readParameter(null, true),
                is(Config.ServiceInstanceParameters.Enrollment.standard));
        assertThat(enrollmentParameterReader.readParameter(null, false),
                is(nullValue()));

        //bad syntax is well thrown
        verifyThrown(() -> enrollmentParameterReader
                        .readParameter("fart", true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.AUTO_ENROLLMENT))));

        //good syntax
        Config.ServiceInstanceParameters.Enrollment enrollment = enrollmentParameterReader
                .readParameter(Config.ServiceInstanceParameters.Enrollment.standard.name(), true);
        assertThat(enrollment, is(equalTo(Config
                .ServiceInstanceParameters.Enrollment.standard)));
        enrollment = enrollmentParameterReader
                .readParameter(Config.ServiceInstanceParameters.Enrollment.forced.name(), false);
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
        assertThat(excludeFromAutoEnrollmentReader.readParameter(null, false),
                is(nullValue()));
        assertThat(excludeFromAutoEnrollmentReader.readParameter("", false),
                is(nullValue()));
        assertThat(excludeFromAutoEnrollmentReader.readParameter(null, false),
                is(nullValue()));

        //bad syntax is well thrown
        verifyThrown(() -> excludeFromAutoEnrollmentReader
                        .readParameter("*",
                                true),
                InvalidParameterException.class,
                parameterChecked ->
                        assertThat(parameterChecked.getParameterName(),
                                is(equalTo(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT))));

        String pattern = ".*";
        Pattern result = excludeFromAutoEnrollmentReader.readParameter(pattern, true);
        assertThat(result, is(not(nullValue())));
        assertThat(result.pattern(), is(equalTo(pattern)));
    }

    @Test
    public void testBuildSecretReader() {
        ParameterReader<String> secretReader = factory.buildSecretReader();
        assertThat(secretReader.getParameterName(), is(equalTo(Config.ServiceInstanceParameters.SECRET)));

        //default case are null
        assertThat(secretReader.readParameter(null, false), is(nullValue()));
        assertThat(secretReader.readParameter(null, false), is(nullValue()));

        String secret = "P@ssword!";
        assertThat(secretReader.readParameter(secret, true), is(equalTo(secret)));
    }
}