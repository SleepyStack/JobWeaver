package com.jobweaver.jobweaverscheduler.exception;

import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerExceptionHierarchyTest {

    @Nested
    @DisplayName("SchedulerException")
    class SchedulerExceptionTests {

        @Test
        @DisplayName("stores all fields correctly")
        void storesFields() {
            UUID jobId = UUID.randomUUID();
            String traceId = UUID.randomUUID().toString();

            SchedulerException ex = new SchedulerException(
                    "test error", ErrorCode.DISPATCH_FAILED, jobId, traceId);

            assertThat(ex.getMessage()).isEqualTo("test error");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.DISPATCH_FAILED);
            assertThat(ex.getJobId()).isEqualTo(jobId);
            assertThat(ex.getTraceId()).isEqualTo(traceId);
            assertThat(ex.getHttpStatus()).isEqualTo(500);
            assertThat(ex.getErrorCode()).isEqualTo("SCHEDULER.DISPATCH_FAILED");
        }

        @Test
        @DisplayName("stores cause when provided")
        void storesCause() {
            RuntimeException cause = new RuntimeException("root cause");
            SchedulerException ex = new SchedulerException(
                    "msg", ErrorCode.DISPATCH_FAILED, null, null, cause);
            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("JobNotFoundException")
    class JobNotFoundTests {

        @Test
        @DisplayName("has correct error code and message")
        void correctCodeAndMessage() {
            UUID jobId = UUID.randomUUID();
            JobNotFoundException ex = new JobNotFoundException(jobId);

            assertThat(ex.getCode()).isEqualTo(ErrorCode.JOB_NOT_FOUND);
            assertThat(ex.getHttpStatus()).isEqualTo(404);
            assertThat(ex.getMessage()).contains(jobId.toString());
        }

        @Test
        @DisplayName("stores traceId when provided")
        void storesTraceId() {
            UUID jobId = UUID.randomUUID();
            String traceId = "trace-123";
            JobNotFoundException ex = new JobNotFoundException(jobId, traceId);
            assertThat(ex.getTraceId()).isEqualTo(traceId);
        }
    }

    @Nested
    @DisplayName("InvalidStateTransitionException")
    class InvalidStateTransitionTests {

        @Test
        @DisplayName("stores from and to status")
        void storesStates() {
            UUID jobId = UUID.randomUUID();
            InvalidStateTransitionException ex = new InvalidStateTransitionException(
                    JobStatus.PENDING, JobStatus.COMPLETED, jobId);

            assertThat(ex.getFromStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(ex.getToStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
            assertThat(ex.getHttpStatus()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("DispatchException")
    class DispatchExceptionTests {

        @Test
        @DisplayName("has DISPATCH_FAILED error code")
        void correctCode() {
            UUID jobId = UUID.randomUUID();
            DispatchException ex = new DispatchException("dispatch failed", jobId, "trace-1");

            assertThat(ex.getCode()).isEqualTo(ErrorCode.DISPATCH_FAILED);
            assertThat(ex.getHttpStatus()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("EventPublishException")
    class EventPublishExceptionTests {

        @Test
        @DisplayName("stores topic")
        void storesTopic() {
            UUID jobId = UUID.randomUUID();
            EventPublishException ex = new EventPublishException(
                    "publish failed", "run-job", jobId, "trace-1");

            assertThat(ex.getTopic()).isEqualTo("run-job");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.EVENT_PUBLISH_FAILED);
        }
    }

    @Nested
    @DisplayName("ErrorCode")
    class ErrorCodeTests {

        @Test
        @DisplayName("nameSpaced returns SCHEDULER-prefixed name")
        void nameSpaced() {
            assertThat(ErrorCode.JOB_NOT_FOUND.nameSpaced()).isEqualTo("SCHEDULER.JOB_NOT_FOUND");
            assertThat(ErrorCode.DISPATCH_FAILED.nameSpaced()).isEqualTo("SCHEDULER.DISPATCH_FAILED");
            assertThat(ErrorCode.EVENT_PUBLISH_FAILED.nameSpaced()).isEqualTo("SCHEDULER.EVENT_PUBLISH_FAILED");
            assertThat(ErrorCode.INVALID_STATE_TRANSITION.nameSpaced()).isEqualTo("SCHEDULER.INVALID_STATE_TRANSITION");
        }

        @Test
        @DisplayName("httpStatus returns correct codes")
        void httpStatus() {
            assertThat(ErrorCode.JOB_NOT_FOUND.httpStatus()).isEqualTo(404);
            assertThat(ErrorCode.INVALID_STATE_TRANSITION.httpStatus()).isEqualTo(409);
            assertThat(ErrorCode.EVENT_PUBLISH_FAILED.httpStatus()).isEqualTo(500);
            assertThat(ErrorCode.DISPATCH_FAILED.httpStatus()).isEqualTo(500);
        }
    }
}
