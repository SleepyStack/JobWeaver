package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.DomainErrorCode;

public enum ErrorCode implements DomainErrorCode {
    JOB_NOT_FOUND(404),
    INVALID_STATE_TRANSITION(409);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    @Override
    public String nameSpaced() {
        return "API." + this.name();
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
