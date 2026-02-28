package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.exception.InvalidStateTransitionException;
import com.jobweaver.jobweaverscheduler.exception.JobNotFoundException;
import com.jobweaver.jobweaverscheduler.kafka.DeadLetterQueuePublisher;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private DeadLetterQueuePublisher deadLetterQueuePublisher;

    @InjectMocks
    private SchedulerService schedulerService;

    private UUID jobId;
    private SimulationInstruction instruction;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        instruction = new SimulationInstruction(List.of(new LogStep("test")));
    }

    private JobExecution createExecution(JobStatus status, int retryCount, int maxRetries) {
        JobExecution exec = new JobExecution(
                jobId,
                UUID.randomUUID().toString(),
                instruction,
                status,
                retryCount,
                maxRetries,
                Instant.now(),
                Instant.now(),
                null
        );
        return exec;
    }

    @Nested
    @DisplayName("markCompleted")
    class MarkCompleted {

        @Test
        @DisplayName("marks RUNNING job as COMPLETED")
        void marksRunningAsCompleted() {
            JobExecution execution = createExecution(JobStatus.RUNNING, 0, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            schedulerService.markCompleted(jobId);

            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        }

        @Test
        @DisplayName("is idempotent - does nothing if already COMPLETED")
        void idempotentWhenAlreadyCompleted() {
            JobExecution execution = createExecution(JobStatus.COMPLETED, 0, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            schedulerService.markCompleted(jobId);

            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        }

        @Test
        @DisplayName("throws InvalidStateTransitionException for PENDING job")
        void throwsForPendingJob() {
            JobExecution execution = createExecution(JobStatus.PENDING, 0, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            assertThatThrownBy(() -> schedulerService.markCompleted(jobId))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("throws InvalidStateTransitionException for FAILED job")
        void throwsForFailedJob() {
            JobExecution execution = createExecution(JobStatus.FAILED, 3, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            assertThatThrownBy(() -> schedulerService.markCompleted(jobId))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("throws JobNotFoundException when job does not exist")
        void throwsWhenNotFound() {
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> schedulerService.markCompleted(jobId))
                    .isInstanceOf(JobNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("handleFailure")
    class HandleFailure {

        @Test
        @DisplayName("schedules retry when retryCount <= maxRetries")
        void schedulesRetry() {
            JobExecution execution = createExecution(JobStatus.RUNNING, 0, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            schedulerService.handleFailure(jobId, "timeout");

            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(execution.getRetryCount()).isEqualTo(1);
            assertThat(execution.getLastError()).isEqualTo("timeout");
            assertThat(execution.getNextRunAt()).isAfter(Instant.now().minusSeconds(1));
            verifyNoInteractions(deadLetterQueuePublisher);
        }

        @Test
        @DisplayName("marks as FAILED and publishes to DLQ when retries exhausted")
        void marksFailedAndPublishesDlq() {
            JobExecution execution = createExecution(JobStatus.RUNNING, 3, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            schedulerService.handleFailure(jobId, "final failure");

            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(execution.getRetryCount()).isEqualTo(4);
            assertThat(execution.getLastError()).isEqualTo("final failure");
            verify(deadLetterQueuePublisher).publish(execution);
        }

        @Test
        @DisplayName("is idempotent - does nothing if not RUNNING")
        void idempotentWhenNotRunning() {
            JobExecution execution = createExecution(JobStatus.PENDING, 0, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            schedulerService.handleFailure(jobId, "error");

            // Status should remain PENDING, no side effects
            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(execution.getRetryCount()).isZero();
            verifyNoInteractions(deadLetterQueuePublisher);
        }

        @Test
        @DisplayName("throws JobNotFoundException when job does not exist")
        void throwsWhenNotFound() {
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> schedulerService.handleFailure(jobId, "error"))
                    .isInstanceOf(JobNotFoundException.class);
        }

        @Test
        @DisplayName("correctly computes exponential backoff delay")
        void exponentialBackoff() {
            // retry 1 -> delay = 5 * 2^1 = 10s
            // retry 2 -> delay = 5 * 2^2 = 20s
            // retry 3 -> delay = 5 * 2^3 = 40s

            JobExecution execution = createExecution(JobStatus.RUNNING, 0, 5);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            Instant before = Instant.now();
            schedulerService.handleFailure(jobId, "error");

            // After first failure, retryCount = 1, delay = 5 * 2^1 = 10s
            assertThat(execution.getNextRunAt()).isAfter(before.plusSeconds(5));
            assertThat(execution.getNextRunAt()).isBefore(before.plusSeconds(15));
        }

        @Test
        @DisplayName("publishes to DLQ with zero max retries")
        void zeroMaxRetries() {
            JobExecution execution = createExecution(JobStatus.RUNNING, 0, 0);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            schedulerService.handleFailure(jobId, "immediate failure");

            assertThat(execution.getJobStatus()).isEqualTo(JobStatus.FAILED);
            verify(deadLetterQueuePublisher).publish(execution);
        }
    }
}
