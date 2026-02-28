package com.jobweaver.api.exceptions;

import java.util.UUID;

/**
 * Thrown when a job lookup by ID yields no result.
 */
public class JobNotFoundException extends ApiException {

    public JobNotFoundException(UUID jobId) {
        super("Job not found: " + jobId, ErrorCode.JOB_NOT_FOUND, jobId);
    }

    public JobNotFoundException(String message, UUID jobId) {
        super(message, ErrorCode.JOB_NOT_FOUND, jobId);
    }
}
