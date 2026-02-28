package com.jobweaver.jobweaverscheduler.exception;

import com.jobweaver.common.exception.BaseDomainException;

import java.util.UUID;

/**
 * Base exception for the scheduler module.
 * Mirrors {@code ApiException} and {@code WorkerException} from their
 * respective modules — carries an {@link ErrorCode} and optional traceId.
 */
public class SchedulerException extends BaseDomainException {

    private final ErrorCode code;
    private final String traceId;

    public SchedulerException(String message, ErrorCode code, UUID jobId, String traceId) {
        super(message, code.nameSpaced(), code.httpStatus(), jobId);
        this.code = code;
        this.traceId = traceId;
    }

    public SchedulerException(String message, ErrorCode code, UUID jobId, String traceId, Throwable cause) {
        super(message, code.nameSpaced(), code.httpStatus(), jobId);
        initCause(cause);
        this.code = code;
        this.traceId = traceId;
    }

    public SchedulerException(String message, ErrorCode code) {
        this(message, code, null, null);
    }

    public ErrorCode getCode() { return code; }

    public String getTraceId() { return traceId; }
}
