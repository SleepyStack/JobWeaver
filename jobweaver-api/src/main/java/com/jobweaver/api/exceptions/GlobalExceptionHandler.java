package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.BaseDomainException;
import com.jobweaver.common.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ── Domain exceptions (ApiException and siblings) ── */

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

    /* ── Spring / Jakarta validation exceptions ── */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return badRequest("API.INVALID_REQUEST", detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));

        return badRequest("API.INVALID_REQUEST", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return badRequest("API.INVALID_REQUEST", "Malformed or unreadable request body");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return badRequest("API.INVALID_REQUEST", "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("API.INVALID_REQUEST",
                "Parameter '" + ex.getName() + "' must be of type " + ex.getRequiredType().getSimpleName());
    }

    /* ── Catch-all ── */

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

    /* ── Helper ── */

    private ResponseEntity<ErrorResponse> badRequest(String errorCode, String message) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                400,
                errorCode,
                message,
                null
        );
        return ResponseEntity.badRequest().body(body);
    }
}
