package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.DomainErrorCode;

public enum ErrorCode implements DomainErrorCode {
    JOB_NOT_FOUND(404),
    INVALID_STATE_TRANSITION(409),
    INVALID_REQUEST(400),
    DUPLICATE_JOB(409),
    EVENT_PUBLISH_FAILED(500);

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
