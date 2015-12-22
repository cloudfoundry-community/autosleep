package org.cloudfoundry.autosleep.dao.model;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ApplicationInfoTest {

    private final Instant yesterday = Instant.now().minus(Duration.ofDays(1));
    private final Instant now = Instant.now();
    private final String appUuid = UUID.randomUUID().toString();



    @SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes", "EqualsWithItself"})
    @Test
    public void testEquals() throws Exception {
        ApplicationInfo sampleApp = getABoundedAppInfo().withRemoteInfo(newApplicationActivity(yesterday, now));
        assertFalse(sampleApp.equals(null));
        assertFalse(sampleApp.equals("toto"));
        assertTrue(sampleApp.equals(sampleApp));

        ApplicationInfo other = getABoundedAppInfo().withRemoteInfo(newApplicationActivity(yesterday, now));
        assertTrue(sampleApp.equals(other));

    }

    private ApplicationInfo getABoundedAppInfo() {
        ApplicationInfo applicationInfo =  getAnAppInfo();
        applicationInfo.getEnrollmentState().addEnrollmentState("AInfoTestServiceId");
        return applicationInfo;
    }

    private ApplicationInfo getAnAppInfo() {
        ApplicationInfo applicationInfo =  new ApplicationInfo(appUuid.toString());
        return applicationInfo;
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(getABoundedAppInfo().withRemoteInfo(newApplicationActivity(yesterday, now)).hashCode()
                == getABoundedAppInfo().withRemoteInfo(newApplicationActivity(yesterday, now)).hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(getABoundedAppInfo().withRemoteInfo(newApplicationActivity(yesterday, now)).toString());
    }

    @Test
    public void testIsWatched() throws Exception {
        ApplicationInfo info = getAnAppInfo();
        assertFalse(info.getEnrollmentState().isWatched());
        String serviceId = "testIsWatched";
        info.getEnrollmentState().addEnrollmentState(serviceId);
        assertTrue(info.getEnrollmentState().isWatched());

        info.getEnrollmentState().updateEnrollment(serviceId, true);
        assertFalse(info.getEnrollmentState().isWatched());

        info.getEnrollmentState().addEnrollmentState(serviceId);
        info.getEnrollmentState().updateEnrollment(serviceId, false);
        assertFalse(info.getEnrollmentState().isWatched());
    }

    private ApplicationActivity newApplicationActivity(Instant lastEvent, Instant lastLog) {
        return new ApplicationActivity(new ApplicationIdentity(appUuid, "appname"),
                CloudApplication.AppState.STARTED, lastEvent, lastLog);
    }
}