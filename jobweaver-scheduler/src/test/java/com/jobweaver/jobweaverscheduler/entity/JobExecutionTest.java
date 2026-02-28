package com.jobweaver.jobweaverscheduler.entity;

import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobExecutionTest {

    private JobExecution execution;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        SimulationInstruction instruction = new SimulationInstruction(List.of(new LogStep("test")));

        execution = new JobExecution(
                jobId,
                "trace-123",
                instruction,
                JobStatus.PENDING,
                0,
                3,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("markAsRunning transitions to RUNNING")
        void markAsRunning() {
            execution.markAsRunning();
            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.RUNNING);
            assertThat(execution.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("markAsCompleted transitions to COMPLETED")
        void markAsCompleted() {
            execution.markAsRunning();
            execution.markAsCompleted();
            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        }

        @Test
        @DisplayName("markAsFailed transitions to FAILED")
        void markAsFailed() {
            execution.markAsRunning();
            execution.markAsFailed();
            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Retry management")
    class RetryManagement {

        @Test
        @DisplayName("incrementRetryCount increments by one")
        void incrementRetryCount() {
            assertThat(execution.getRetryCount()).isZero();
            execution.incrementRetryCount();
            assertThat(execution.getRetryCount()).isEqualTo(1);
            execution.incrementRetryCount();
            assertThat(execution.getRetryCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("scheduleRetry sets PENDING status and nextRunAt")
        void scheduleRetry() {
            execution.markAsRunning();
            Instant nextRun = Instant.now().plusSeconds(60);

            execution.scheduleRetry(nextRun);

            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(execution.getNextRunAt()).isEqualTo(nextRun);
            assertThat(execution.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("setError stores error message")
        void setError() {
            execution.setError("Something went wrong");
            assertThat(execution.getLastError()).isEqualTo("Something went wrong");
        }

        @Test
        @DisplayName("setError can overwrite previous error")
        void overwriteError() {
            execution.setError("first error");
            execution.setError("second error");
            assertThat(execution.getLastError()).isEqualTo("second error");
        }
    }

    @Nested
    @DisplayName("Constructor and getters")
    class ConstructorAndGetters {

        @Test
        @DisplayName("all fields initialized correctly")
        void allFieldsCorrect() {
            assertThat(execution.getJobId()).isEqualTo(jobId);
            assertThat(execution.getTraceId()).isEqualTo("trace-123");
            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(execution.getRetryCount()).isZero();
            assertThat(execution.getMaxRetries()).isEqualTo(3);
            assertThat(execution.getLastError()).isNull();
        }

        @Test
        @DisplayName("no-arg constructor creates instance")
        void noArgConstructor() {
            JobExecution empty = new JobExecution();
            assertThat(empty).isNotNull();
        }
    }
}
