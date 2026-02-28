package com.jobweaver.api.exceptions;

import java.util.UUID;

/**
 * Thrown when a requested state transition is not permitted by the
 * job state machine (e.g. moving a {@code FAILED} job back to {@code RUNNING}).
 */
public class InvalidStateTransitionException extends ApiException {

    private final String fromState;
    private final String toState;

    public InvalidStateTransitionException(String fromState, String toState, UUID jobId) {
        super("Invalid state transition from " + fromState + " to " + toState + " for job " + jobId,
                ErrorCode.INVALID_STATE_TRANSITION, jobId);
        this.fromState = fromState;
        this.toState = toState;
    }

    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
}
