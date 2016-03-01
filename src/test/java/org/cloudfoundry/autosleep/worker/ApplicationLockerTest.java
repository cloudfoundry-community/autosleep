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
    public void testExecuteThreadSafe() throws Exception {
        Duration duration = Duration.ofMillis(500);
        ArrayList<Integer> someInts = new ArrayList<>();
        Runnable task1 = spy(new SleepingRun(1, duration.dividedBy(5).toMillis(), someInts));
        Runnable task2 = spy(new DryRun(2, someInts));
        applicationLocker.executeThreadSafe("someId", task1);
        Thread.sleep(duration.dividedBy(10).toMillis());
        applicationLocker.executeThreadSafe("someId", task2);
        Thread.sleep(duration.toMillis());
        verify(task1, times(1)).run();
        verify(task2, times(1)).run();
        assertThat(someInts.size(), is(equalTo(2)));
        assertThat(someInts.get(0), is(equalTo(1)));
        assertThat(someInts.get(1), is(equalTo(2)));
    }

    @Test
    public void testRemoveApplication() throws Exception {
        Runnable task1 = () -> log.debug("passed");
        applicationLocker.executeThreadSafe("someId", task1);
        //call on existing task does not throw exception
        applicationLocker.removeApplication("someId");
        //call again neither
        applicationLocker.removeApplication("someId");
        //Call on non existing neither
        applicationLocker.removeApplication("someOtherId");


    }


}