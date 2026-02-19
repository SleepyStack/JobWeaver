package com.jobweaver.common.exception;

import java.util.UUID;

public class BaseDomainException extends RuntimeException {
    private final String errorCode;
    private final UUID jobId;
    protected BaseDomainException(String message, String errorCode, UUID jobId) {
        super(message);
        this.errorCode = errorCode;
        this.jobId = jobId;
    }
    public String getErrorCode() { return errorCode; }
    public UUID getJobId() { return jobId; }

}
