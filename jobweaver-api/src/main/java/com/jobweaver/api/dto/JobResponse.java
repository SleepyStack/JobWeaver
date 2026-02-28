package com.jobweaver.api.dto;

import com.jobweaver.api.entity.Job;
import com.jobweaver.common.messaging.enumeration.JobType;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a persisted {@link Job}.
 */
public record JobResponse(
        UUID id,
        JobType type,
        String traceId,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getTraceId(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
