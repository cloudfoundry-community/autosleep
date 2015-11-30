package org.cloudfoundry.autosleep.dao.model;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ApplicationInfoTest {

    private final Instant yesterday = Instant.now().minus(Duration.ofDays(1));
    private final Instant now = Instant.now();
    private final UUID appUuid = UUID.randomUUID();


    @Test
    public void testSerialization() {
        RedisSerializer<ApplicationInfo> serializer = new Jackson2JsonRedisSerializer<>(ApplicationInfo.class);
        ApplicationInfo origin = getAnApplicationInfo().withRemoteInfo(newApplicationActivity(yesterday, now));
        byte[] serialized = serializer.serialize(origin);
        ApplicationInfo retrieved = serializer.deserialize(serialized);
        log.debug("Object origin = {}", origin);
        log.debug("Object retrieved = {}", retrieved);
        assertThat("Serialization and deserialisation should return the same object ", origin, is(equalTo(retrieved)));

        //test serialization when nextCheck not null
        origin.markAsChecked(Instant.now());
        serialized = serializer.serialize(origin);
        retrieved = serializer.deserialize(serialized);
        log.debug("Object origin = {}", origin);
        log.debug("Object retrieved = {}", retrieved);
        assertThat("Serialization and deserialisation should return the same object ", origin, is(equalTo(retrieved)));
    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes", "EqualsWithItself"})
    @Test
    public void testEquals() throws Exception {
        ApplicationInfo sampleApp = getAnApplicationInfo().withRemoteInfo(newApplicationActivity(yesterday, now));
        assertFalse(sampleApp.equals(null));
        assertFalse(sampleApp.equals("toto"));
        assertTrue(sampleApp.equals(sampleApp));

        ApplicationInfo other = getAnApplicationInfo().withRemoteInfo(newApplicationActivity(yesterday, now));
        assertTrue(sampleApp.equals(other));

        other.updateRemoteInfo(newApplicationActivity(now, yesterday));
        assertFalse(sampleApp.equals(other));
    }

    private ApplicationInfo getAnApplicationInfo() {
        ApplicationInfo applicationInfo =  new ApplicationInfo(appUuid);
        applicationInfo.addBoundService("AInfoTestServiceId");
        return applicationInfo;
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(getAnApplicationInfo().withRemoteInfo(newApplicationActivity(yesterday, now)).hashCode()
                == getAnApplicationInfo().withRemoteInfo(newApplicationActivity(yesterday, now)).hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(getAnApplicationInfo().withRemoteInfo(newApplicationActivity(yesterday, now)).toString());
    }

    private ApplicationActivity newApplicationActivity(Instant lastEvent, Instant lastLog) {
        return new ApplicationActivity(new ApplicationIdentity(appUuid, "appname"),
                CloudApplication.AppState.STARTED, lastEvent, lastLog);
    }
}