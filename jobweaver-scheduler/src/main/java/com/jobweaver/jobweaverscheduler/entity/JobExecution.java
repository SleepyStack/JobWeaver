package com.jobweaver.jobweaverscheduler.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "job_executions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_execution_job_id", columnNames = "jobId")
        },
        indexes = {
                @Index(name = "idx_execution_status", columnList = "jobStatus"),
                @Index(name = "idx_execution_next_run", columnList = "nextRunAt"),
                @Index(name = "idx_execution_trace_id", columnList = "traceId")
        }
)
public class JobExecution {

    @Id
    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private String traceId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetries;

    @Column(nullable = false)
    private Instant nextRunAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private String lastError;
}
