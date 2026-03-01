package com.jobweaver.jobweaverscheduler.dto;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobExecutionResponse(
        UUID jobId,
        String traceId,
        JobStatus jobStatus,
        int retryCount,
        int maxRetries,
        Instant nextRunAt,
        Instant updatedAt,
        String lastError) {
    public static JobExecutionResponse from(JobExecution execution) {
        return new JobExecutionResponse(
                execution.getJobId(),
                execution.getTraceId(),
                execution.getJobStatus(),
                execution.getRetryCount(),
                execution.getMaxRetries(),
                execution.getNextRunAt(),
                execution.getUpdatedAt(),
                execution.getLastError());
    }
}
