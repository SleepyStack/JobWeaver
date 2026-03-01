package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.jobweaverscheduler.dto.JobExecutionResponse;
import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.exception.JobNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobExecutionQueryServiceTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @InjectMocks
    private JobExecutionQueryService queryService;

    private UUID jobId;
    private SimulationInstruction instruction;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        instruction = new SimulationInstruction(List.of(new LogStep("test")));
    }

    private JobExecution createExecution(UUID id, JobStatus status, int retryCount, int maxRetries) {
        return new JobExecution(
                id,
                UUID.randomUUID().toString(),
                instruction,
                status,
                retryCount,
                maxRetries,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Nested
    @DisplayName("getJobStatus")
    class GetJobStatus {

        @Test
        @DisplayName("returns status for existing job")
        void returnsStatusForExistingJob() {
            JobExecution execution = createExecution(jobId, JobStatus.RUNNING, 1, 3);
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(execution));

            JobExecutionResponse response = queryService.getJobStatus(jobId);

            assertThat(response.jobId()).isEqualTo(jobId);
            assertThat(response.jobStatus()).isEqualTo(JobStatus.RUNNING);
            assertThat(response.retryCount()).isEqualTo(1);
            assertThat(response.maxRetries()).isEqualTo(3);
        }

        @Test
        @DisplayName("throws JobNotFoundException for unknown job")
        void throwsForUnknownJob() {
            when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getJobStatus(jobId))
                    .isInstanceOf(JobNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listByStatus")
    class ListByStatus {

        @Test
        @DisplayName("returns only jobs matching the given status")
        void filtersbyStatus() {
            JobExecution pending = createExecution(UUID.randomUUID(), JobStatus.PENDING, 0, 3);
            when(jobExecutionRepository.findByJobStatus(JobStatus.PENDING)).thenReturn(List.of(pending));

            List<JobExecutionResponse> results = queryService.listByStatus(JobStatus.PENDING);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).jobStatus()).isEqualTo(JobStatus.PENDING);
        }

        @Test
        @DisplayName("returns empty list when no jobs match")
        void returnsEmptyWhenNoMatch() {
            when(jobExecutionRepository.findByJobStatus(JobStatus.FAILED)).thenReturn(List.of());

            List<JobExecutionResponse> results = queryService.listByStatus(JobStatus.FAILED);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("returns all executions")
        void returnsAll() {
            JobExecution a = createExecution(UUID.randomUUID(), JobStatus.PENDING, 0, 3);
            JobExecution b = createExecution(UUID.randomUUID(), JobStatus.COMPLETED, 0, 3);
            when(jobExecutionRepository.findAll()).thenReturn(List.of(a, b));

            List<JobExecutionResponse> results = queryService.listAll();

            assertThat(results).hasSize(2);
        }
    }
}
