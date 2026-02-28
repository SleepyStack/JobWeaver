package com.jobweaver.jobweaverscheduler.exception;

import java.util.UUID;

/**
 * Thrown when a job execution record is not found in the scheduler database.
 */
public class JobNotFoundException extends SchedulerException {

    public JobNotFoundException(UUID jobId) {
        super("Job execution not found: " + jobId, ErrorCode.JOB_NOT_FOUND, jobId, null);
    }

    public JobNotFoundException(UUID jobId, String traceId) {
        super("Job execution not found: " + jobId, ErrorCode.JOB_NOT_FOUND, jobId, traceId);
    }
}
