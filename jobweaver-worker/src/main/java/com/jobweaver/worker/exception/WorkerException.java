package com.jobweaver.worker.exception;

import com.jobweaver.common.exception.BaseDomainException;

import java.util.UUID;

public class WorkerException extends BaseDomainException {

    private final ErrorCode code;
    private final String traceId;

    public WorkerException(String message, ErrorCode code, UUID jobId, String traceId) {
        super(message, code.nameSpaced(), code.httpStatus(), jobId);
        this.code = code;
        this.traceId = traceId;
    }

    public WorkerException(String message, ErrorCode code, UUID jobId, String traceId, Throwable cause) {
        super(message, code.nameSpaced(), code.httpStatus(), jobId);
        initCause(cause);
        this.code = code;
        this.traceId = traceId;
    }

    public WorkerException(String message, ErrorCode code) {
        this(message, code, null, null);
    }

    public ErrorCode getCode() { return code; }

    public String getTraceId() { return traceId; }
}
