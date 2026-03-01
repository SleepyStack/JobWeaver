package com.jobweaver.worker.dto;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import com.jobweaver.worker.entity.ExecutionAttempt;

import java.time.Instant;
import java.util.UUID;

public record ExecutionAttemptResponse(
        UUID eventId,
        UUID jobId,
        String traceId,
        ExecutionOutcome outcome,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage) {
    public static ExecutionAttemptResponse from(ExecutionAttempt attempt) {
        return new ExecutionAttemptResponse(
                attempt.getEventId(),
                attempt.getJobId(),
                attempt.getTraceId(),
                attempt.getOutcome(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getErrorMessage());
    }
}
