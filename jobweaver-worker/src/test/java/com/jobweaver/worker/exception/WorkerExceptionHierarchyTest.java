package com.jobweaver.worker.exception;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerExceptionHierarchyTest {

    @Nested
    @DisplayName("WorkerException")
    class WorkerExceptionTests {

        @Test
        @DisplayName("stores all fields correctly")
        void storesFields() {
            UUID jobId = UUID.randomUUID();
            String traceId = "trace-123";

            WorkerException ex = new WorkerException(
                    "test error", ErrorCode.SIMULATION_FAILED, jobId, traceId);

            assertThat(ex.getMessage()).isEqualTo("test error");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.SIMULATION_FAILED);
            assertThat(ex.getJobId()).isEqualTo(jobId);
            assertThat(ex.getTraceId()).isEqualTo(traceId);
            assertThat(ex.getHttpStatus()).isEqualTo(500);
            assertThat(ex.getErrorCode()).isEqualTo("WORKER.SIMULATION_FAILED");
        }
    }

    @Nested
    @DisplayName("SimulationFailureException")
    class SimulationFailureTests {

        @Test
        @DisplayName("stores stepType")
        void storesStepType() {
            UUID jobId = UUID.randomUUID();
            SimulationFailureException ex = new SimulationFailureException(
                    "crash", "FAIL", jobId, "trace");

            assertThat(ex.getStepType()).isEqualTo("FAIL");
            assertThat(ex.getCode()).isEqualTo(ErrorCode.SIMULATION_FAILED);
        }
    }

    @Nested
    @DisplayName("SimulationInterruptedException")
    class SimulationInterruptedTests {

        @Test
        @DisplayName("stores stepType and cause")
        void storesStepTypeAndCause() {
            UUID jobId = UUID.randomUUID();
            InterruptedException cause = new InterruptedException("interrupted");

            SimulationInterruptedException ex = new SimulationInterruptedException(
                    "sleep interrupted", "SLEEP", jobId, "trace", cause);

            assertThat(ex.getStepType()).isEqualTo("SLEEP");
            assertThat(ex.getCause()).isEqualTo(cause);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.SIMULATION_INTERRUPTED);
        }
    }

    @Nested
    @DisplayName("EventPublishException")
    class EventPublishTests {

        @Test
        @DisplayName("stores topic")
        void storesTopic() {
            UUID jobId = UUID.randomUUID();
            RuntimeException cause = new RuntimeException("kafka down");

            EventPublishException ex = new EventPublishException(
                    "publish failed", "job-completed", jobId, "trace", cause);

            assertThat(ex.getTopic()).isEqualTo("job-completed");
            assertThat(ex.getCause()).isEqualTo(cause);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.EVENT_PUBLISH_FAILED);
        }
    }

    @Nested
    @DisplayName("MalformedRecordException")
    class MalformedRecordTests {

        @Test
        @DisplayName("stores header name, partition, and offset")
        void storesAllFields() {
            TopicPartition tp = new TopicPartition("run-job", 2);

            MalformedRecordException ex = new MalformedRecordException(
                    "Missing header", "traceId", tp, 42);

            assertThat(ex.getHeaderName()).isEqualTo("traceId");
            assertThat(ex.getTopicPartition()).isEqualTo(tp);
            assertThat(ex.getOffset()).isEqualTo(42);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.MALFORMED_RECORD);
        }
    }

    @Nested
    @DisplayName("OffsetCommitException")
    class OffsetCommitTests {

        @Test
        @DisplayName("stores partition and offset")
        void storesFields() {
            TopicPartition tp = new TopicPartition("run-job", 1);
            RuntimeException cause = new RuntimeException("commit failed");

            OffsetCommitException ex = new OffsetCommitException(
                    "Failed to commit", tp, 100, cause);

            assertThat(ex.getTopicPartition()).isEqualTo(tp);
            assertThat(ex.getOffset()).isEqualTo(100);
            assertThat(ex.getCause()).isEqualTo(cause);
            assertThat(ex.getCode()).isEqualTo(ErrorCode.OFFSET_COMMIT_FAILED);
        }
    }

    @Nested
    @DisplayName("ErrorCode")
    class ErrorCodeTests {

        @Test
        @DisplayName("nameSpaced returns WORKER-prefixed names")
        void nameSpaced() {
            assertThat(ErrorCode.JOB_NOT_FOUND.nameSpaced()).isEqualTo("WORKER.JOB_NOT_FOUND");
            assertThat(ErrorCode.SIMULATION_FAILED.nameSpaced()).isEqualTo("WORKER.SIMULATION_FAILED");
            assertThat(ErrorCode.SIMULATION_INTERRUPTED.nameSpaced()).isEqualTo("WORKER.SIMULATION_INTERRUPTED");
            assertThat(ErrorCode.EVENT_PUBLISH_FAILED.nameSpaced()).isEqualTo("WORKER.EVENT_PUBLISH_FAILED");
            assertThat(ErrorCode.MALFORMED_RECORD.nameSpaced()).isEqualTo("WORKER.MALFORMED_RECORD");
            assertThat(ErrorCode.OFFSET_COMMIT_FAILED.nameSpaced()).isEqualTo("WORKER.OFFSET_COMMIT_FAILED");
        }

        @Test
        @DisplayName("httpStatus returns correct codes")
        void httpStatus() {
            assertThat(ErrorCode.JOB_NOT_FOUND.httpStatus()).isEqualTo(404);
            assertThat(ErrorCode.SIMULATION_FAILED.httpStatus()).isEqualTo(500);
            assertThat(ErrorCode.MALFORMED_RECORD.httpStatus()).isEqualTo(400);
            assertThat(ErrorCode.OFFSET_COMMIT_FAILED.httpStatus()).isEqualTo(500);
        }
    }
}
