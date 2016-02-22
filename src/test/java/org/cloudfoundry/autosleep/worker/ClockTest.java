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

package org.cloudfoundry.autosleep.worker;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ClockTest {

    private static final Duration PERIOD = Duration.ofMillis(200);

    private static final Duration SLEEP_TIME_BEFORE_TEST = PERIOD.dividedBy(3);

    private static final String TEST_ID = "93847";

    protected Clock clock = new Clock();

    @Mock
    private Runnable runnable;


    @Test
    public void testStartTask() throws Exception {
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        Thread.sleep(SLEEP_TIME_BEFORE_TEST.toMillis());
        verify(runnable, never()).run();
        Thread.sleep(PERIOD.toMillis());
        verify(runnable, times(1)).run();
        Thread.sleep(PERIOD.toMillis());
        verify(runnable, times(1)).run();
    }

    @Test
    public void testRemoveTask() throws Exception {
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        clock.removeTask(TEST_ID);
        assertThat(clock.listTaskIds().size(), is(equalTo(0)));

    }

    @Test
    public void testListTaskIds() throws Exception {
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        Set<String> taskIds = clock.listTaskIds();
        assertThat(taskIds, is(notNullValue()));
        assertThat(taskIds.size(), is(equalTo(1)));
        assertTrue(taskIds.contains(TEST_ID));
    }
}
