package org.cloudfoundry.autosleep.dao.model;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationStateMachineTest {

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Before
    public void initMockLogger() {
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger
                .ROOT_LOGGER_NAME);
        root.addAppender(mockAppender);
    }

    @Test
    public void testOnOptOut() throws Exception {
        ApplicationStateMachine stateMachine = new ApplicationStateMachine();
        assertThat(stateMachine.getState(), is(equalTo(ApplicationStateMachine.State.MONITORED)));

        //authorized transition
        stateMachine.onOptOut();
        assertThat(stateMachine.getState(), is(equalTo(ApplicationStateMachine.State.IGNORED)));
        verify(mockAppender, never()).doAppend(any());

        //unauthorized transition
        stateMachine.onOptOut();
        verify(mockAppender, times(1)).doAppend(any());
        assertThat(stateMachine.getState(), is(equalTo(ApplicationStateMachine.State.IGNORED)));

    }

    @Test
    public void testOnOptIn() throws Exception {
        ApplicationStateMachine stateMachine = new ApplicationStateMachine();
        assertThat(stateMachine.getState(), is(equalTo(ApplicationStateMachine.State.MONITORED)));
        stateMachine.onOptOut();

        //authorized transition
        stateMachine.onOptIn();
        verify(mockAppender, never()).doAppend(any());
        assertThat(stateMachine.getState(), is(equalTo(ApplicationStateMachine.State.MONITORED)));

        //unauthorized transition
        stateMachine.onOptIn();
        verify(mockAppender, times(1)).doAppend(any());
        assertThat(stateMachine.getState(), is(equalTo(ApplicationStateMachine.State.MONITORED)));
    }
}