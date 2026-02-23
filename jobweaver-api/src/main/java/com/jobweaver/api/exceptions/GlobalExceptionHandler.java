package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.BaseDomainException;
import com.jobweaver.common.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(BaseDomainException ex) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                ex.getHttpStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getJobId()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                500,
                "API.INTERNAL_ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(500).body(body);
    }
}
