package org.cloudfoundry.autosleep.dao.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ApplicationStateMachine {

    public enum State {MONITORED, IGNORED}

    private State state;

    public ApplicationStateMachine() {
        this.state = State.MONITORED;
    }

    public void onOptOut() {
        switch (this.state) {
            case MONITORED:
                this.state = State.IGNORED;
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
            case IGNORED:
                this.state = State.MONITORED;
                break;
            default:
                log.error("Unauthorized transition");
                break;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ApplicationStateMachine)) {
            return false;
        }
        ApplicationStateMachine other = (ApplicationStateMachine) object;
        return this.getState() == other.getState();
    }

    @Override
    public int hashCode() {
        return getState().hashCode();
    }
}
