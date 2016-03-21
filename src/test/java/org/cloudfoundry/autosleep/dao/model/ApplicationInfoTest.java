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
    public void test_default_is_not_watched() throws Exception {
        //Given a default application info
        ApplicationInfo info = BeanGenerator.createAppInfo();

        //When asked if watched
        //Then it returns false
        assertFalse(info.getEnrollmentState().isWatched());
    }

    @Test
    public void test_adding_a_service_move_enrollment_to_watched() throws Exception {
        //Given a default application info
        ApplicationInfo info = BeanGenerator.createAppInfo();
        String serviceId = "testIsWatched";
        //When adding a service
        info.getEnrollmentState().addEnrollmentState(serviceId);
        //Then application is watched
        assertTrue(info.getEnrollmentState().isWatched());
    }

    @Test
    public void test_blacklist_on_single_existing_set_not_watched() throws Exception {
        //Given a default application info that is watched by a service
        ApplicationInfo info = BeanGenerator.createAppInfo();
        String serviceId = "testIsWatched";
        info.getEnrollmentState().addEnrollmentState(serviceId);

        //When removing the service
        info.getEnrollmentState().updateEnrollment(serviceId, true);

        //Then applications is no longuer watched
        assertFalse(info.getEnrollmentState().isWatched());


    }

}