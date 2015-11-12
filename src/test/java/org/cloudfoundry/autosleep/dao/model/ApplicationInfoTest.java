package org.cloudfoundry.autosleep.dao.model;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ApplicationInfoTest {

    private final Instant yesterday = Instant.now().minus(Duration.ofDays(1));
    private final Instant now = Instant.now();
    private final UUID appUuid = UUID.randomUUID();

    @Test
    public void testGetLastEventTime() throws Exception {
        ApplicationInfo info = new ApplicationInfo(newCloudApp(), appUuid, yesterday, now);
        assertThat("Most recent date should be last log", info.computeLastDate(), is(equalTo(info.getLastLog())));
        assertThat("Last event should return most recent date", info.computeLastDate(), is(equalTo(now)));

        info = new ApplicationInfo(newCloudApp(), appUuid, now, yesterday);
        assertThat("Last event should return most recent date", info.computeLastDate(), is(equalTo(now)));
        assertThat("Most recent date should be last deployed", info.getLastEvent(),
                is(equalTo(info.computeLastDate())));

        info = new ApplicationInfo(newCloudApp(), appUuid, now, null);
        assertThat("Last event should not be null", info.computeLastDate(), is(equalTo(now)));

        info = new ApplicationInfo(newCloudApp(), appUuid, null, now);
        assertThat("Last event should not be null", info.computeLastDate(), is(equalTo(now)));

        info = new ApplicationInfo(newCloudApp(), appUuid, null, null);
        assertThat("Last event should rbe null", info.computeLastDate(), is(nullValue()));

    }

    @Test
    public void testSerialization() {
        RedisSerializer<ApplicationInfo> serializer = new Jackson2JsonRedisSerializer<>(ApplicationInfo.class);
        ApplicationInfo origin = new ApplicationInfo(newCloudApp(), appUuid, now, yesterday);
        byte[] serialized = serializer.serialize(origin);
        ApplicationInfo retrieved = serializer.deserialize(serialized);
        log.debug("Object origin = {}", origin);
        log.debug("Object retrieved = {}", retrieved);
        assertThat("Serialization and deserialisation should return the same object ", origin, is(equalTo(retrieved)));

        //test serialization when nextCheck not null
        origin.setNextCheck(Instant.now());
        serialized = serializer.serialize(origin);
        retrieved = serializer.deserialize(serialized);
        log.debug("Object origin = {}", origin);
        log.debug("Object retrieved = {}", retrieved);
        assertThat("Serialization and deserialisation should return the same object ", origin, is(equalTo(retrieved)));
    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes", "EqualsWithItself"})
    @Test
    public void testEquals() throws Exception {
        ApplicationInfo sampleApp = new ApplicationInfo(newCloudApp(), appUuid, now, yesterday);
        assertFalse(sampleApp.equals(null));
        assertFalse(sampleApp.equals("toto"));
        assertTrue(sampleApp.equals(sampleApp));
        assertTrue(sampleApp.equals(new ApplicationInfo(newCloudApp(), appUuid, now, yesterday)));
        assertFalse(sampleApp.equals(new ApplicationInfo(newCloudApp(), appUuid, yesterday, now)));
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(new ApplicationInfo(newCloudApp(), appUuid, now, yesterday).hashCode()
                == new ApplicationInfo(newCloudApp(), appUuid, now, yesterday).hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(new ApplicationInfo(newCloudApp(), appUuid, now, yesterday).toString());
    }

    private CloudApplication newCloudApp() {
        CloudApplication app = new CloudApplication("appname", null, null, 1024, 3,
                Arrays.asList("uri1", "uri2"), null, CloudApplication.AppState.STARTED);
        app.setSpace(new CloudSpace(null, "mySpace", new CloudOrganization(null, "myOrg")));
        return app;
    }
}