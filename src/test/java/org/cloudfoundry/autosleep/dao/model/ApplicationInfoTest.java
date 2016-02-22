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

package org.cloudfoundry.autosleep.dao.model;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ApplicationInfoTest {

    @Test
    public void testIsWatched() throws Exception {
        ApplicationInfo info = BeanGenerator.createAppInfo();
        assertFalse(info.getEnrollmentState().isWatched());
        String serviceId = "testIsWatched";
        info.getEnrollmentState().addEnrollmentState(serviceId);
        assertTrue(info.getEnrollmentState().isWatched());

        info.getEnrollmentState().updateEnrollment(serviceId, true);
        assertFalse(info.getEnrollmentState().isWatched());

        info.getEnrollmentState().addEnrollmentState(serviceId);
        info.getEnrollmentState().updateEnrollment(serviceId, false);
        assertFalse(info.getEnrollmentState().isWatched());
    }

}