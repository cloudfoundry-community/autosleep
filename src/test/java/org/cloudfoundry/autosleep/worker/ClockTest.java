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

package org.cloudfoundry.autosleep.worker;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.worker.scheduling.Clock;
import org.cloudfoundry.autosleep.worker.scheduling.TimeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ClockTest {

    private static final Duration PERIOD = Duration.ofMillis(200);
    
    private static final String TEST_ID = "93847";

    @InjectMocks
    protected Clock clock;

    @Mock
    private Runnable runnable;

    @Mock
    private TimeManager timeManager;

    @Test
    public void test_list_tasks_ids() throws Exception {
        //Given scheduler contains a task
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        //When we ask the list of task ids
        Set<String> taskIds = clock.listTaskIds();
        //Then the list is not null
        assertThat(taskIds, is(notNullValue()));
        //And it contains one task id that is the one we gave
        assertThat(taskIds.size(), is(equalTo(1)));
        assertTrue(taskIds.contains(TEST_ID));
    }

    @Test
    public void test_remove_task() throws Exception {
        //Given scheduler contains a task
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        //When we remove the task
        clock.removeTask(TEST_ID);
        //Then clock does not contain any more task
        assertThat(clock.listTaskIds().size(), is(equalTo(0)));

    }

    @Test
    public void test_schedule_task_calls_scheduler() throws Exception {
        //Given nothing
        //When we schedule a task
        clock.scheduleTask(TEST_ID, PERIOD, runnable);
        //It does not run immediately
        verify(timeManager, times(1)).schedule(eq(runnable), eq(PERIOD));
    }
}
