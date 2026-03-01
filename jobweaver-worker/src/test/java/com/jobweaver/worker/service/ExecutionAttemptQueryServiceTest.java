package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import com.jobweaver.worker.dto.ExecutionAttemptResponse;
import com.jobweaver.worker.entity.ExecutionAttempt;
import com.jobweaver.worker.repository.ExecutionAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionAttemptQueryServiceTest {

    @Mock
    private ExecutionAttemptRepository executionAttemptRepository;

    @InjectMocks
    private ExecutionAttemptQueryService queryService;

    @Test
    @DisplayName("returns execution attempts for a job ordered by startedAt desc")
    void returnsAttemptsForJob() {
        UUID jobId = UUID.randomUUID();
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();

        ExecutionAttempt attempt1 = new ExecutionAttempt(
                eventId1, jobId, "trace-1", Instant.now().minusSeconds(60),
                ExecutionOutcome.FAILURE, "step failed");
        ExecutionAttempt attempt2 = new ExecutionAttempt(
                eventId2, jobId, "trace-1", Instant.now(),
                ExecutionOutcome.SUCCESS, null);

        when(executionAttemptRepository.findByJobIdOrderByStartedAtDesc(jobId))
                .thenReturn(List.of(attempt2, attempt1));

        List<ExecutionAttemptResponse> results = queryService.getAttemptsByJobId(jobId);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).eventId()).isEqualTo(eventId2);
        assertThat(results.get(0).outcome()).isEqualTo(ExecutionOutcome.SUCCESS);
        assertThat(results.get(1).eventId()).isEqualTo(eventId1);
        assertThat(results.get(1).outcome()).isEqualTo(ExecutionOutcome.FAILURE);
        assertThat(results.get(1).errorMessage()).isEqualTo("step failed");
    }

    @Test
    @DisplayName("returns empty list for unknown job")
    void returnsEmptyForUnknownJob() {
        UUID unknownJobId = UUID.randomUUID();
        when(executionAttemptRepository.findByJobIdOrderByStartedAtDesc(unknownJobId))
                .thenReturn(List.of());

        List<ExecutionAttemptResponse> results = queryService.getAttemptsByJobId(unknownJobId);

        assertThat(results).isEmpty();
    }
}
