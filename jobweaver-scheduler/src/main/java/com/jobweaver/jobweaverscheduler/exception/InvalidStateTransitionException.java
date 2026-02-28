package com.jobweaver.jobweaverscheduler.exception;

import com.jobweaver.jobweaverscheduler.entity.JobStatus;

import java.util.UUID;

/**
 * Thrown when a state transition is not permitted by the job state machine
 * (e.g. moving a {@code PENDING} job directly to {@code COMPLETED}).
 */
public class InvalidStateTransitionException extends SchedulerException {

    private final JobStatus fromStatus;
    private final JobStatus toStatus;

    public InvalidStateTransitionException(JobStatus fromStatus, JobStatus toStatus, UUID jobId) {
        super("Invalid state transition from " + fromStatus + " to " + toStatus + " for job " + jobId,
                ErrorCode.INVALID_STATE_TRANSITION, jobId, null);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public InvalidStateTransitionException(JobStatus fromStatus, JobStatus toStatus, UUID jobId, String traceId) {
        super("Invalid state transition from " + fromStatus + " to " + toStatus + " for job " + jobId,
                ErrorCode.INVALID_STATE_TRANSITION, jobId, traceId);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public JobStatus getFromStatus() { return fromStatus; }
    public JobStatus getToStatus() { return toStatus; }
}
