package com.jobweaver.api.exceptions;

import com.jobweaver.common.exception.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHierarchyTest {

    @Nested
    @DisplayName("ApiException")
    class ApiExceptionTests {

        @Test
        @DisplayName("stores message and error code")
        void storesFields() {
            ApiException ex = new ApiException("test", ErrorCode.INVALID_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("test");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            assertThat(ex.getHttpStatus()).isEqualTo(400);
            assertThat(ex.getErrorCode()).isEqualTo("API.INVALID_REQUEST");
        }

        @Test
        @DisplayName("stores jobId when provided")
        void storesJobId() {
            UUID jobId = UUID.randomUUID();
            ApiException ex = new ApiException("test", ErrorCode.JOB_NOT_FOUND, jobId);
            assertThat(ex.getJobId()).isEqualTo(jobId);
        }
    }

    @Nested
    @DisplayName("InvalidJobRequestException")
    class InvalidJobRequestExceptionTests {

        @Test
        @DisplayName("has INVALID_REQUEST error code and stores field")
        void hasCorrectCode() {
            InvalidJobRequestException ex = new InvalidJobRequestException("bad field", "fieldName");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
            assertThat(ex.getField()).isEqualTo("fieldName");
            assertThat(ex.getHttpStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("field can be null")
        void nullField() {
            InvalidJobRequestException ex = new InvalidJobRequestException("bad");
            assertThat(ex.getField()).isNull();
        }
    }

    @Nested
    @DisplayName("JobNotFoundException")
    class JobNotFoundExceptionTests {

        @Test
        @DisplayName("has JOB_NOT_FOUND error code and 404 status")
        void hasCorrectCode() {
            UUID jobId = UUID.randomUUID();
            JobNotFoundException ex = new JobNotFoundException(jobId);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.JOB_NOT_FOUND);
            assertThat(ex.getHttpStatus()).isEqualTo(404);
            assertThat(ex.getJobId()).isEqualTo(jobId);
            assertThat(ex.getMessage()).contains(jobId.toString());
        }
    }

    @Nested
    @DisplayName("DuplicateJobException")
    class DuplicateJobExceptionTests {

        @Test
        @DisplayName("has DUPLICATE_JOB error code and 409 status")
        void hasCorrectCode() {
            UUID jobId = UUID.randomUUID();
            DuplicateJobException ex = new DuplicateJobException(jobId);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.DUPLICATE_JOB);
            assertThat(ex.getHttpStatus()).isEqualTo(409);
            assertThat(ex.getJobId()).isEqualTo(jobId);
        }
    }

    @Nested
    @DisplayName("InvalidStateTransitionException")
    class InvalidStateTransitionTests {

        @Test
        @DisplayName("stores from and to states")
        void storesStates() {
            UUID jobId = UUID.randomUUID();
            InvalidStateTransitionException ex =
                    new InvalidStateTransitionException("PENDING", "COMPLETED", jobId);
            assertThat(ex.getFromState()).isEqualTo("PENDING");
            assertThat(ex.getToState()).isEqualTo("COMPLETED");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
            assertThat(ex.getHttpStatus()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("EventPublishException")
    class EventPublishExceptionTests {

        @Test
        @DisplayName("stores topic and chained cause")
        void storesTopicAndCause() {
            UUID jobId = UUID.randomUUID();
            RuntimeException cause = new RuntimeException("kafka down");
            EventPublishException ex =
                    new EventPublishException("publish failed", "job-created", jobId, cause);
            assertThat(ex.getTopic()).isEqualTo("job-created");
            assertThat(ex.getCause()).isEqualTo(cause);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.EVENT_PUBLISH_FAILED);
            assertThat(ex.getHttpStatus()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("ErrorCode")
    class ErrorCodeTests {

        @Test
        @DisplayName("nameSpaced returns API-prefixed name")
        void nameSpaced() {
            assertThat(ErrorCode.JOB_NOT_FOUND.nameSpaced()).isEqualTo("API.JOB_NOT_FOUND");
            assertThat(ErrorCode.INVALID_REQUEST.nameSpaced()).isEqualTo("API.INVALID_REQUEST");
            assertThat(ErrorCode.DUPLICATE_JOB.nameSpaced()).isEqualTo("API.DUPLICATE_JOB");
            assertThat(ErrorCode.EVENT_PUBLISH_FAILED.nameSpaced()).isEqualTo("API.EVENT_PUBLISH_FAILED");
        }

        @Test
        @DisplayName("httpStatus returns correct status codes")
        void httpStatus() {
            assertThat(ErrorCode.JOB_NOT_FOUND.httpStatus()).isEqualTo(404);
            assertThat(ErrorCode.INVALID_REQUEST.httpStatus()).isEqualTo(400);
            assertThat(ErrorCode.INVALID_STATE_TRANSITION.httpStatus()).isEqualTo(409);
            assertThat(ErrorCode.DUPLICATE_JOB.httpStatus()).isEqualTo(409);
            assertThat(ErrorCode.EVENT_PUBLISH_FAILED.httpStatus()).isEqualTo(500);
        }
    }
}
