package com.jobweaver.worker.exception;

import com.jobweaver.common.exception.BaseDomainException;

import java.util.UUID;

public class WorkerException extends BaseDomainException {
    private final ErrorCode code;

    public WorkerException(String message, ErrorCode code, UUID jobId) {
        super(message, code.nameSpaced(), code.httpStatus(), jobId);
        this.code = code;
    }

    public ErrorCode getCode() { return code; }
}
