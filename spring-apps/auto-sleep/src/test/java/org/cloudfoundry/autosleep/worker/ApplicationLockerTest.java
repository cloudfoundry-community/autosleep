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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.util.ApplicationLocker;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class ApplicationLockerTest {

    private ApplicationLocker applicationLocker = spy(new ApplicationLocker());

    @AllArgsConstructor
    private static class SleepingRun implements Runnable {
        private int id;

        private long sleepTime;

        private List<Integer> output;

        @Override
        public void run() {
            try {
                Thread.sleep(sleepTime);
                output.add(id);
            } catch (InterruptedException i) {
                log.debug("passed");
            }
        }
    }

    @AllArgsConstructor
    private static class DryRun implements Runnable {
        private int id;

        private List<Integer> output;

        @Override
        public void run() {
            output.add(id);
        }
    }


    @Test
    public void test_execute_thread_safe_locks_properly() throws Exception {
        Duration duration = Duration.ofMillis(500);
        ArrayList<Integer> someInts = new ArrayList<>();
        //Given a task that will wait a little before adding its id
        Runnable task1 = spy(new SleepingRun(1, duration.dividedBy(5).toMillis(), someInts));
        //And another one that will add it immediately
        Runnable task2 = spy(new DryRun(2, someInts));
        //When we launch the sleeping one before the second one
        applicationLocker.executeThreadSafe("someId", task1);
        applicationLocker.executeThreadSafe("someId", task2);
        //Then all tasks are run
        verify(task1, times(1)).run();
        verify(task2, times(1)).run();
        //And the ids of task have been inserted in the good order
        assertThat(someInts.size(), is(equalTo(2)));
        //The first one is the first that called the executeThreadSafe even if it slept
        assertThat(someInts.get(0), is(equalTo(1)));
        //And the second one is the dry one
        assertThat(someInts.get(1), is(equalTo(2)));
    }

    @Test
    public void test_remove_applications_succeed() throws Exception {
        Runnable task1 = () -> log.debug("passed");
        //Given a lock exist
        applicationLocker.executeThreadSafe("someId", task1);
        //When we call a remove
        applicationLocker.removeApplication("someId");
        //Then no exception is thrown
    }



}