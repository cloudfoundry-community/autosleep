package org.cloudfoundry.autosleep.repositories.redis;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class RedisUtil {

    private static final int REDIS_DEFAULT_PORT = 6379;

    public static boolean isRedisPresent() {
        try {
            new Socket("localhost", REDIS_DEFAULT_PORT);
        } catch (IOException e) {
            log.debug("Redis not running...");
            return false;
        }
        return true;
    }
}
