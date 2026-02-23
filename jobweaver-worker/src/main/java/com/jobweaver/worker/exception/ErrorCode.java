package com.jobweaver.worker.exception;

import com.jobweaver.common.exception.DomainErrorCode;

public enum ErrorCode implements DomainErrorCode {
    JOB_NOT_FOUND(404);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    @Override
    public String nameSpaced() {
        return "WORKER." + this.name();
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
