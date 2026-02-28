package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.common.messaging.simulation.SleepStep;
import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @InjectMocks
    private IngestionService ingestionService;

    @Captor
    private ArgumentCaptor<JobExecution> executionCaptor;

    private SimulationInstruction instruction;
    private UUID jobId;
    private String traceId;

    @BeforeEach
    void setUp() {
        instruction = new SimulationInstruction(
                List.of(new SleepStep(100), new LogStep("test"))
        );
        jobId = UUID.randomUUID();
        traceId = UUID.randomUUID().toString();
    }

    @Nested
    @DisplayName("persistIfNotExists")
    class PersistIfNotExists {

        @Test
        @DisplayName("saves new JobExecution with correct fields")
        void savesNewExecution() {
            JobCreatedEvent event = new JobCreatedEvent(jobId, JobType.SIMULATION, instruction, 3);

            ingestionService.persistIfNotExists(event, traceId);

            verify(jobExecutionRepository).save(executionCaptor.capture());
            JobExecution saved = executionCaptor.getValue();

            assertThat(saved.getJobId()).isEqualTo(jobId);
            assertThat(saved.getTraceId()).isEqualTo(traceId);
            assertThat(saved.getInstruction()).isEqualTo(instruction);
            assertThat(saved.getJobStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(saved.getRetryCount()).isZero();
            assertThat(saved.getMaxRetries()).isEqualTo(3);
            assertThat(saved.getNextRunAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getLastError()).isNull();
        }

        @Test
        @DisplayName("silently ignores DataIntegrityViolationException (duplicates)")
        void ignoresDuplicate() {
            JobCreatedEvent event = new JobCreatedEvent(jobId, JobType.SIMULATION, instruction, 3);
            when(jobExecutionRepository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            // Should not throw
            ingestionService.persistIfNotExists(event, traceId);

            verify(jobExecutionRepository).save(any());
        }

        @Test
        @DisplayName("propagates non-DataIntegrity exceptions")
        void propagatesOtherExceptions() {
            JobCreatedEvent event = new JobCreatedEvent(jobId, JobType.SIMULATION, instruction, 3);
            when(jobExecutionRepository.save(any()))
                    .thenThrow(new RuntimeException("DB down"));

            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> ingestionService.persistIfNotExists(event, traceId)
            );
        }

        @Test
        @DisplayName("sets maxRetries from event")
        void setsMaxRetries() {
            JobCreatedEvent event = new JobCreatedEvent(jobId, JobType.SIMULATION, instruction, 5);

            ingestionService.persistIfNotExists(event, traceId);

            verify(jobExecutionRepository).save(executionCaptor.capture());
            assertThat(executionCaptor.getValue().getMaxRetries()).isEqualTo(5);
        }
    }
}
