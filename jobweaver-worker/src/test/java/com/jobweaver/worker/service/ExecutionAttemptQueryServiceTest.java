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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("returns execution attempts for a job paginated by startedAt desc")
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

                Page<ExecutionAttempt> page = new PageImpl<>(List.of(attempt2, attempt1), pageable, 2);
                when(executionAttemptRepository.findByJobId(jobId, pageable))
                                .thenReturn(page);

                Page<ExecutionAttemptResponse> results = queryService.getAttemptsByJobId(jobId, pageable);

                assertThat(results.getContent()).hasSize(2);
                assertThat(results.getContent().get(0).eventId()).isEqualTo(eventId2);
                assertThat(results.getContent().get(0).outcome()).isEqualTo(ExecutionOutcome.SUCCESS);
                assertThat(results.getContent().get(1).eventId()).isEqualTo(eventId1);
                assertThat(results.getContent().get(1).outcome()).isEqualTo(ExecutionOutcome.FAILURE);
                assertThat(results.getContent().get(1).errorMessage()).isEqualTo("step failed");
                assertThat(results.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("returns empty page for unknown job")
        void returnsEmptyForUnknownJob() {
                UUID unknownJobId = UUID.randomUUID();
                Page<ExecutionAttempt> emptyPage = new PageImpl<>(List.of(), pageable, 0);
                when(executionAttemptRepository.findByJobId(unknownJobId, pageable))
                                .thenReturn(emptyPage);

                Page<ExecutionAttemptResponse> results = queryService.getAttemptsByJobId(unknownJobId, pageable);

                assertThat(results.getContent()).isEmpty();
                assertThat(results.getTotalElements()).isZero();
        }
}
