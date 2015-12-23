package org.cloudfoundry.autosleep.dao.model;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.util.BeanGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ApplicationInfoTest {

    @Test
    public void testIsWatched() throws Exception {
        ApplicationInfo info = BeanGenerator.createAppInfo();
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

}