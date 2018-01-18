package org.cloudfoundry.autosleep.util;

import ch.qos.logback.classic.LoggerContext;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TestLogBackConfig {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestLogBackConfig.class);

    @Test
    public void logBack_displays_exception_with_jar_names() {
        Exception rootCause = new Exception("root cause");
        Exception exception = new Exception("sample exception", rootCause);
        log.info("inspect manually the displayed stack trace contains jars", exception);
    }


}
