package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.BaseDomainException;

import java.util.UUID;

public class ApiException extends BaseDomainException {
    private final ErrorCode code;

    public ApiException(String message, ErrorCode code, UUID jobId) {
        super(message, code.nameSpaced(), code.httpStatus(), jobId);
        this.code = code;
    }

    public ApiException(String message, ErrorCode code) {
        this(message, code, null);
    }

    public ErrorCode getCode() { return code; }
}
