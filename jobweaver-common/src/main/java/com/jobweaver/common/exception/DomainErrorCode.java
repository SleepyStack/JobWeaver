package com.jobweaver.common.exception;

public interface DomainErrorCode {
    String nameSpaced();
    int httpStatus();
}
