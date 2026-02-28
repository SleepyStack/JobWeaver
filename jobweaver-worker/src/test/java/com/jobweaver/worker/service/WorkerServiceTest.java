package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.worker.kafka.WorkerEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private ExecutionAttemptProcessor attemptProcessor;

    @Mock
    private WorkerEventPublisher eventPublisher;

    @InjectMocks
    private WorkerService workerService;

    private UUID eventId;
    private UUID jobId;
    private String traceId;
    private RunJobEvent event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        traceId = UUID.randomUUID().toString();
        SimulationInstruction instruction = new SimulationInstruction(List.of(new LogStep("test")));
        event = new RunJobEvent(jobId, instruction);
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("publishes success event on successful processing")
        void successfulProcessing() {
            when(attemptProcessor.executeTransaction(eventId, traceId, event))
                    .thenReturn(ProcessingResult.ofSuccess());

            workerService.process(eventId, traceId, event);

            verify(eventPublisher).publishSuccess(jobId, traceId);
            verify(eventPublisher, never()).publishFailure(any(), any(), any());
        }

        @Test
        @DisplayName("publishes failure event on failed processing")
        void failedProcessing() {
            when(attemptProcessor.executeTransaction(eventId, traceId, event))
                    .thenReturn(ProcessingResult.ofFailure("timeout"));

            workerService.process(eventId, traceId, event);

            verify(eventPublisher).publishFailure(jobId, "timeout", traceId);
            verify(eventPublisher, never()).publishSuccess(any(), any());
        }

        @Test
        @DisplayName("republishes success for duplicate success")
        void duplicateSuccess() {
            when(attemptProcessor.executeTransaction(eventId, traceId, event))
                    .thenReturn(ProcessingResult.ofDuplicateSuccess());

            workerService.process(eventId, traceId, event);

            verify(eventPublisher).publishSuccess(jobId, traceId);
        }

        @Test
        @DisplayName("republishes failure for duplicate failure")
        void duplicateFailure() {
            when(attemptProcessor.executeTransaction(eventId, traceId, event))
                    .thenReturn(ProcessingResult.ofDuplicateFailure("old error"));

            workerService.process(eventId, traceId, event);

            verify(eventPublisher).publishFailure(jobId, "old error", traceId);
        }

        @Test
        @DisplayName("skips publishing for duplicate running")
        void duplicateRunning() {
            when(attemptProcessor.executeTransaction(eventId, traceId, event))
                    .thenReturn(ProcessingResult.ofDuplicateRunning());

            workerService.process(eventId, traceId, event);

            verifyNoInteractions(eventPublisher);
        }
    }
}
