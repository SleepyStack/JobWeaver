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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        instruction = new SimulationInstruction(List.of(new LogStep("test")));
        pageable = PageRequest.of(0, 20);
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
        void filtersByStatus() {
            JobExecution pending = createExecution(UUID.randomUUID(), JobStatus.PENDING, 0, 3);
            Page<JobExecution> page = new PageImpl<>(List.of(pending), pageable, 1);
            when(jobExecutionRepository.findByJobStatus(JobStatus.PENDING, pageable)).thenReturn(page);

            Page<JobExecutionResponse> results = queryService.listByStatus(JobStatus.PENDING, pageable);

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).jobStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(results.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns empty page when no jobs match")
        void returnsEmptyWhenNoMatch() {
            Page<JobExecution> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(jobExecutionRepository.findByJobStatus(JobStatus.FAILED, pageable)).thenReturn(emptyPage);

            Page<JobExecutionResponse> results = queryService.listByStatus(JobStatus.FAILED, pageable);

            assertThat(results.getContent()).isEmpty();
            assertThat(results.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("returns all executions paginated")
        void returnsAll() {
            JobExecution a = createExecution(UUID.randomUUID(), JobStatus.PENDING, 0, 3);
            JobExecution b = createExecution(UUID.randomUUID(), JobStatus.COMPLETED, 0, 3);
            Page<JobExecution> page = new PageImpl<>(List.of(a, b), pageable, 2);
            when(jobExecutionRepository.findAll(pageable)).thenReturn(page);

            Page<JobExecutionResponse> results = queryService.listAll(pageable);

            assertThat(results.getContent()).hasSize(2);
            assertThat(results.getTotalElements()).isEqualTo(2);
        }
    }
}
