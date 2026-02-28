package com.jobweaver.worker.entity;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionAttemptTest {

    private ExecutionAttempt attempt;
    private UUID eventId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        attempt = new ExecutionAttempt(
                eventId, jobId, "trace-1", Instant.now(), ExecutionOutcome.RUNNING, null
        );
    }

    @Nested
    @DisplayName("Constructor and getters")
    class ConstructorAndGetters {

        @Test
        @DisplayName("all fields initialized correctly")
        void allFieldsCorrect() {
            assertThat(attempt.getEventId()).isEqualTo(eventId);
            assertThat(attempt.getJobId()).isEqualTo(jobId);
            assertThat(attempt.getTraceId()).isEqualTo("trace-1");
            assertThat(attempt.getOutcome()).isEqualTo(ExecutionOutcome.RUNNING);
            assertThat(attempt.getErrorMessage()).isNull();
            assertThat(attempt.getStartedAt()).isNotNull();
            assertThat(attempt.getFinishedAt()).isNull();
        }

        @Test
        @DisplayName("no-arg constructor creates instance")
        void noArgConstructor() {
            ExecutionAttempt empty = new ExecutionAttempt();
            assertThat(empty).isNotNull();
        }
    }

    @Nested
    @DisplayName("markSuccess")
    class MarkSuccess {

        @Test
        @DisplayName("transitions to SUCCESS and sets finishedAt")
        void marksSuccess() {
            attempt.markSuccess();

            assertThat(attempt.getOutcome()).isEqualTo(ExecutionOutcome.SUCCESS);
            assertThat(attempt.getFinishedAt()).isNotNull();
            assertThat(attempt.getErrorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("markFailure")
    class MarkFailure {

        @Test
        @DisplayName("transitions to FAILURE with error message and sets finishedAt")
        void marksFailure() {
            attempt.markFailure("something went wrong");

            assertThat(attempt.getOutcome()).isEqualTo(ExecutionOutcome.FAILURE);
            assertThat(attempt.getFinishedAt()).isNotNull();
            assertThat(attempt.getErrorMessage()).isEqualTo("something went wrong");
        }
    }
}
