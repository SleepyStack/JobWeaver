package com.jobweaver.api.service;

import com.jobweaver.api.dto.JobPage;
import com.jobweaver.api.dto.JobResponse;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.exceptions.JobNotFoundException;
import com.jobweaver.api.kafka.JobEventPublisher;
import com.jobweaver.api.repository.JobRepository;
import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceQueryTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobEventPublisher publisher;

    @InjectMocks
    private JobService jobService;

    private final SimulationInstruction instruction =
            new SimulationInstruction(List.of(new LogStep("test")));

    private Job createJob(UUID id, JobType type) {
        Job job = new Job(type, instruction, UUID.randomUUID().toString());
        setField(job, "id", id);
        setField(job, "createdAt", Instant.now());
        setField(job, "updatedAt", Instant.now());
        return job;
    }

    // ───────────────── GET by ID ─────────────────

    @Nested
    @DisplayName("getJob")
    class GetJob {

        @Test
        @DisplayName("returns job when found")
        void returnsJobWhenFound() {
            UUID id = UUID.randomUUID();
            Job job = createJob(id, JobType.SIMULATION);

            when(jobRepository.findById(id)).thenReturn(Optional.of(job));

            JobResponse response = jobService.getJob(id);

            assertThat(response.id()).isEqualTo(id);
            assertThat(response.type()).isEqualTo(JobType.SIMULATION);
            assertThat(response.traceId()).isEqualTo(job.getTraceId());
            assertThat(response.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("throws JobNotFoundException when not found")
        void throwsWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(jobRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.getJob(id))
                    .isInstanceOf(JobNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ───────────────── List jobs ─────────────────

    @Nested
    @DisplayName("listJobs")
    class ListJobs {

        @Test
        @DisplayName("returns paginated results without type filter")
        void returnsAllJobs() {
            Job job1 = createJob(UUID.randomUUID(), JobType.SIMULATION);
            Job job2 = createJob(UUID.randomUUID(), JobType.SIMULATION);

            Page<Job> page = new PageImpl<>(
                    List.of(job1, job2),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    2
            );
            when(jobRepository.findAll(any(Pageable.class))).thenReturn(page);

            JobPage result = jobService.listJobs(null, 0, 20);

            assertThat(result.jobs()).hasSize(2);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.totalPages()).isEqualTo(1);

            verify(jobRepository).findAll(any(Pageable.class));
            verify(jobRepository, never()).findByType(any(), any());
        }

        @Test
        @DisplayName("filters by type when provided")
        void filtersByType() {
            Job job = createJob(UUID.randomUUID(), JobType.SIMULATION);

            Page<Job> page = new PageImpl<>(
                    List.of(job),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );
            when(jobRepository.findByType(eq(JobType.SIMULATION), any(Pageable.class))).thenReturn(page);

            JobPage result = jobService.listJobs(JobType.SIMULATION, 0, 10);

            assertThat(result.jobs()).hasSize(1);
            assertThat(result.jobs().getFirst().type()).isEqualTo(JobType.SIMULATION);

            verify(jobRepository).findByType(eq(JobType.SIMULATION), any(Pageable.class));
            verify(jobRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("returns empty page when no jobs match")
        void returnsEmptyPage() {
            Page<Job> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );
            when(jobRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            JobPage result = jobService.listJobs(null, 0, 20);

            assertThat(result.jobs()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.totalPages()).isZero();
        }

        @Test
        @DisplayName("maps Job entity fields correctly to JobResponse")
        void mapsFieldsCorrectly() {
            UUID id = UUID.randomUUID();
            Job job = createJob(id, JobType.SIMULATION);

            Page<Job> page = new PageImpl<>(List.of(job),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
            when(jobRepository.findAll(any(Pageable.class))).thenReturn(page);

            JobPage result = jobService.listJobs(null, 0, 20);

            JobResponse resp = result.jobs().getFirst();
            assertThat(resp.id()).isEqualTo(id);
            assertThat(resp.type()).isEqualTo(JobType.SIMULATION);
            assertThat(resp.traceId()).isEqualTo(job.getTraceId());
            assertThat(resp.createdAt()).isEqualTo(job.getCreatedAt());
            assertThat(resp.updatedAt()).isEqualTo(job.getUpdatedAt());
        }
    }

    // ───────────────── Helper ─────────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
