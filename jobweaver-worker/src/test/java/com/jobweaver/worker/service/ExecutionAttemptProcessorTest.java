package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.common.messaging.simulation.FailStep;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.worker.entity.ExecutionAttempt;
import com.jobweaver.worker.exception.SimulationFailureException;
import com.jobweaver.worker.repository.ExecutionAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionAttemptProcessorTest {

    @Mock
    private ExecutionAttemptRepository repository;

    @Mock
    private SimulationExecutor executor;

    @InjectMocks
    private ExecutionAttemptProcessor processor;

    @Captor
    private ArgumentCaptor<ExecutionAttempt> attemptCaptor;

    private UUID eventId;
    private UUID jobId;
    private String traceId;
    private RunJobEvent event;
    private SimulationInstruction instruction;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        traceId = UUID.randomUUID().toString();
        instruction = new SimulationInstruction(List.of(new LogStep("test")));
        event = new RunJobEvent(jobId, instruction);
    }

    @Nested
    @DisplayName("executeTransaction - new event")
    class NewEvent {

        @Test
        @DisplayName("creates attempt, executes, and returns success")
        void successfulExecution() {
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            ProcessingResult result = processor.executeTransaction(eventId, traceId, event);

            assertThat(result.success()).isTrue();
            assertThat(result.duplicate()).isFalse();
            assertThat(result.failure()).isFalse();

            verify(repository).save(attemptCaptor.capture());
            ExecutionAttempt saved = attemptCaptor.getValue();
            assertThat(saved.getEventId()).isEqualTo(eventId);
            assertThat(saved.getJobId()).isEqualTo(jobId);
            assertThat(saved.getTraceId()).isEqualTo(traceId);
            assertThat(saved.getOutcome()).isEqualTo(ExecutionOutcome.SUCCESS);

            verify(executor).execute(instruction, jobId, traceId);
        }

        @Test
        @DisplayName("marks failure when executor throws")
        void failedExecution() {
            when(repository.findById(eventId)).thenReturn(Optional.empty());
            doThrow(new SimulationFailureException("crash", "FAIL", jobId, traceId))
                    .when(executor).execute(any(), any(), any());

            ProcessingResult result = processor.executeTransaction(eventId, traceId, event);

            assertThat(result.failure()).isTrue();
            assertThat(result.success()).isFalse();
            assertThat(result.duplicate()).isFalse();
            assertThat(result.errorMessage()).isEqualTo("crash");

            verify(repository).save(attemptCaptor.capture());
            ExecutionAttempt saved = attemptCaptor.getValue();
            assertThat(saved.getOutcome()).isEqualTo(ExecutionOutcome.FAILURE);
        }
    }

    @Nested
    @DisplayName("executeTransaction - duplicate event")
    class DuplicateEvent {

        @Test
        @DisplayName("returns duplicate success for already-succeeded attempt")
        void duplicateSuccess() {
            ExecutionAttempt existing = new ExecutionAttempt(
                    eventId, jobId, traceId, Instant.now(), ExecutionOutcome.SUCCESS, null);
            when(repository.findById(eventId)).thenReturn(Optional.of(existing));

            ProcessingResult result = processor.executeTransaction(eventId, traceId, event);

            assertThat(result.duplicate()).isTrue();
            assertThat(result.success()).isTrue();
            verifyNoMoreInteractions(executor);
        }

        @Test
        @DisplayName("returns duplicate failure for already-failed attempt")
        void duplicateFailure() {
            ExecutionAttempt existing = new ExecutionAttempt(
                    eventId, jobId, traceId, Instant.now(), ExecutionOutcome.FAILURE, "previous error");
            when(repository.findById(eventId)).thenReturn(Optional.of(existing));

            ProcessingResult result = processor.executeTransaction(eventId, traceId, event);

            assertThat(result.duplicate()).isTrue();
            assertThat(result.failure()).isTrue();
            assertThat(result.errorMessage()).isEqualTo("previous error");
            verifyNoMoreInteractions(executor);
        }

        @Test
        @DisplayName("returns duplicate running for in-progress attempt")
        void duplicateRunning() {
            ExecutionAttempt existing = new ExecutionAttempt(
                    eventId, jobId, traceId, Instant.now(), ExecutionOutcome.RUNNING, null);
            when(repository.findById(eventId)).thenReturn(Optional.of(existing));

            ProcessingResult result = processor.executeTransaction(eventId, traceId, event);

            assertThat(result.duplicate()).isTrue();
            assertThat(result.success()).isFalse();
            assertThat(result.failure()).isFalse();
            verifyNoMoreInteractions(executor);
        }
    }
}
