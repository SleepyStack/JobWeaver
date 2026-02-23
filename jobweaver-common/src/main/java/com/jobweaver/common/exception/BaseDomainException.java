package com.jobweaver.common.exception;

import java.util.UUID;

public class BaseDomainException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;
    private final UUID jobId;

    protected BaseDomainException(String message, String errorCode, int httpStatus, UUID jobId) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.jobId = jobId;
    }

    public String getErrorCode() { return errorCode; }
    public int getHttpStatus() { return httpStatus; }
    public UUID getJobId() { return jobId; }
}
