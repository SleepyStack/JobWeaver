package com.jobweaver.api.exceptions;

/**
 * Thrown when business-level validation of a job request fails
 * (e.g. invalid maxRetryCount, unsupported job type, malformed payload).
 * Distinct from Spring's bean-validation errors which are handled
 * separately in {@link GlobalExceptionHandler}.
 */
public class InvalidJobRequestException extends ApiException {

    private final String field;

    public InvalidJobRequestException(String message, String field) {
        super(message, ErrorCode.INVALID_REQUEST);
        this.field = field;
    }

    public InvalidJobRequestException(String message) {
        this(message, null);
    }

    public String getField() { return field; }
}
