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
