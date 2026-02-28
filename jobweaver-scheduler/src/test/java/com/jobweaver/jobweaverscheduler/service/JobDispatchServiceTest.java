package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.kafka.RunJobPublisher;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDispatchServiceTest {

    @Mock
    private JobExecutionRepository repository;

    @Mock
    private RunJobPublisher publisher;

    @InjectMocks
    private JobDispatchService jobDispatchService;

    private SimulationInstruction instruction;

    @BeforeEach
    void setUp() {
        instruction = new SimulationInstruction(List.of(new LogStep("test")));
    }

    private JobExecution createPendingExecution() {
        return new JobExecution(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                instruction,
                JobStatus.PENDING,
                0,
                3,
                Instant.now().minusSeconds(10),
                Instant.now(),
                null
        );
    }

    @Nested
    @DisplayName("dispatchPendingJobs")
    class DispatchPendingJobs {

        @Test
        @DisplayName("marks each job as RUNNING and publishes RunJobEvent")
        void dispatchesJobs() {
            JobExecution job1 = createPendingExecution();
            JobExecution job2 = createPendingExecution();
            when(repository.findReadyJobs(any(Instant.class)))
                    .thenReturn(List.of(job1, job2));

            jobDispatchService.dispatchPendingJobs();

            assertThat(job1.getJobStatus()).isEqualTo(JobStatus.RUNNING);
            assertThat(job2.getJobStatus()).isEqualTo(JobStatus.RUNNING);

            verify(publisher).publishRunJob(
                    job1.getJobId(), job1.getTraceId(), job1.getInstruction());
            verify(publisher).publishRunJob(
                    job2.getJobId(), job2.getTraceId(), job2.getInstruction());
        }

        @Test
        @DisplayName("does nothing when no ready jobs")
        void noReadyJobs() {
            when(repository.findReadyJobs(any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            jobDispatchService.dispatchPendingJobs();

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("continues dispatching remaining jobs when one fails")
        void continuesOnFailure() {
            JobExecution job1 = createPendingExecution();
            JobExecution job2 = createPendingExecution();
            when(repository.findReadyJobs(any(Instant.class)))
                    .thenReturn(List.of(job1, job2));

            doThrow(new RuntimeException("Kafka error")).when(publisher)
                    .publishRunJob(eq(job1.getJobId()), anyString(), any());

            jobDispatchService.dispatchPendingJobs();

            // job2 should still be dispatched despite job1 failure
            verify(publisher).publishRunJob(
                    job2.getJobId(), job2.getTraceId(), job2.getInstruction());
        }

        @Test
        @DisplayName("marks job as RUNNING before publishing")
        void marksRunningBeforePublish() {
            JobExecution job = createPendingExecution();
            when(repository.findReadyJobs(any(Instant.class)))
                    .thenReturn(List.of(job));

            doAnswer(invocation -> {
                // At this point the job should already be marked as RUNNING
                assertThat(job.getJobStatus()).isEqualTo(JobStatus.RUNNING);
                return null;
            }).when(publisher).publishRunJob(any(), anyString(), any());

            jobDispatchService.dispatchPendingJobs();
        }
    }
}
