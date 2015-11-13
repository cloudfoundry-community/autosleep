package org.cloudfoundry.autosleep.dao.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ApplicationStateMachine {
    public enum State {MONITORED, OPTED_OUT};
    private State state;

    public ApplicationStateMachine() {
        this.state = State.MONITORED;
    }

    public void onOptOut() {
        switch (this.state) {
            case MONITORED:
                this.state = State.OPTED_OUT;
                break;
            default:
                log.error("Unauthorized transition");
                break;
        }
    }

    /**
     * When an app was manually unbound, and then re-bound.
     */
    public void onOptIn() {
        switch (this.state) {
            case OPTED_OUT:
                this.state = State.MONITORED;
                break;
            default:
                log.error("Unauthorized transition");
                break;
        }
    }
}
