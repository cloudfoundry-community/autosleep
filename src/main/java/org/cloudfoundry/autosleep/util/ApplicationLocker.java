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

package org.cloudfoundry.autosleep.util;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ApplicationLocker {

    private Map<String, Lock> locks = new HashMap<>();

    public void executeThreadSafe(String applicationId, Runnable reneEbel) {
        Lock lock = locks.get(applicationId);
        if (lock == null) {
            synchronized (locks) {
                lock = locks.get(applicationId);
                if (lock == null) {
                    lock = new ReentrantLock(false);
                    locks.put(applicationId, lock);
                }
            }
        }
        lock.lock();
        try {
            reneEbel.run();
        } finally {
            lock.unlock();
        }
    }

    public void removeApplication(String applicationId) {
        synchronized (locks) {
            locks.remove(applicationId);
        }
    }

}
