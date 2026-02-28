package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleDomainException")
    class DomainException {

        @Test
        @DisplayName("returns correct status and body for ApiException")
        void handleApiException() {
            UUID jobId = UUID.randomUUID();
            ApiException ex = new JobNotFoundException(jobId);

            ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("API.JOB_NOT_FOUND");
            assertThat(response.getBody().jobId()).isEqualTo(jobId);
            assertThat(response.getBody().message()).contains(jobId.toString());
        }

        @Test
        @DisplayName("returns 400 for InvalidJobRequestException")
        void handleInvalidRequest() {
            InvalidJobRequestException ex = new InvalidJobRequestException("bad input", "field");

            ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().errorCode()).isEqualTo("API.INVALID_REQUEST");
        }

        @Test
        @DisplayName("returns 500 for EventPublishException")
        void handleEventPublishException() {
            UUID jobId = UUID.randomUUID();
            EventPublishException ex = new EventPublishException(
                    "publish failed", "topic", jobId);

            ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().errorCode()).isEqualTo("API.EVENT_PUBLISH_FAILED");
        }
    }

    @Nested
    @DisplayName("handleUnexpected")
    class UnexpectedExceptions {

        @Test
        @DisplayName("returns 500 with INTERNAL_ERROR code")
        void handleGenericException() {
            Exception ex = new RuntimeException("boom");

            ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("API.INTERNAL_ERROR");
            assertThat(response.getBody().message()).isEqualTo("boom");
            assertThat(response.getBody().jobId()).isNull();
        }
    }

    @Nested
    @DisplayName("handleUnreadable")
    class UnreadableBody {

        @Test
        @DisplayName("returns 400 with descriptive message")
        void handleUnreadable() {
            org.springframework.http.converter.HttpMessageNotReadableException ex =
                    new org.springframework.http.converter.HttpMessageNotReadableException(
                            "Cannot parse", (org.springframework.http.HttpInputMessage) null);

            ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().errorCode()).isEqualTo("API.INVALID_REQUEST");
            assertThat(response.getBody().message()).isEqualTo("Malformed or unreadable request body");
        }
    }
}
