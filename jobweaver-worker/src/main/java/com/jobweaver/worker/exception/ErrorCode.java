package com.jobweaver.worker.exception;

import com.jobweaver.common.exception.DomainErrorCode;

public enum ErrorCode implements DomainErrorCode {

    JOB_NOT_FOUND(404),

    SIMULATION_FAILED(500),
    SIMULATION_INTERRUPTED(500),

    EVENT_PUBLISH_FAILED(500),

    MALFORMED_RECORD(400),

    OFFSET_COMMIT_FAILED(500);

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
