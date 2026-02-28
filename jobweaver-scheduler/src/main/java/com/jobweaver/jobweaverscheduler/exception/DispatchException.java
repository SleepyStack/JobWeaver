package com.jobweaver.jobweaverscheduler.exception;

import java.util.UUID;

/**
 * Thrown when dispatching a pending job to the worker fails
 * (e.g. mark-as-running succeeds but the Kafka publish does not).
 */
public class DispatchException extends SchedulerException {

    public DispatchException(String message, UUID jobId, String traceId, Throwable cause) {
        super(message, ErrorCode.DISPATCH_FAILED, jobId, traceId, cause);
    }

    public DispatchException(String message, UUID jobId, String traceId) {
        super(message, ErrorCode.DISPATCH_FAILED, jobId, traceId);
    }
}
