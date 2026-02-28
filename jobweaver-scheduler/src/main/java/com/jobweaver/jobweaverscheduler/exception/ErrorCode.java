package com.jobweaver.jobweaverscheduler.exception;

import com.jobweaver.common.exception.DomainErrorCode;

public enum ErrorCode implements DomainErrorCode {

    JOB_NOT_FOUND(404),

    INVALID_STATE_TRANSITION(409),

    EVENT_PUBLISH_FAILED(500),

    DISPATCH_FAILED(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    @Override
    public String nameSpaced() {
        return "SCHEDULER." + this.name();
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
