package com.jobweaver.api.exceptions;

import java.util.UUID;

/**
 * Thrown when a duplicate job submission is detected
 * (e.g. idempotency key collision or duplicate traceId).
 */
public class DuplicateJobException extends ApiException {

    public DuplicateJobException(UUID jobId) {
        super("Duplicate job submission: " + jobId, ErrorCode.DUPLICATE_JOB, jobId);
    }

    public DuplicateJobException(String message, UUID jobId) {
        super(message, ErrorCode.DUPLICATE_JOB, jobId);
    }
}
