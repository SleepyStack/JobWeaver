package com.jobweaver.jobweaverscheduler.entity;

import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
        name = "job_executions",
        indexes = {
                @Index(name = "idx_execution_status", columnList = "jobStatus"),
                @Index(name = "idx_execution_next_run", columnList = "nextRunAt"),
                @Index(name = "idx_execution_trace_id", columnList = "traceId")
        }
)
@NoArgsConstructor
@Getter
public class JobExecution {

    @Id
    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb",  nullable = false)
    private SimulationInstruction instruction;

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

    @Version
    private long version;

    public JobExecution(UUID jobId, String traceId, SimulationInstruction instruction, JobStatus jobStatus, int retryCount, int maxRetries, Instant nextRunAt, Instant updatedAt, String lastError) {
        this.jobId = jobId;
        this.traceId = traceId;
        this.instruction = instruction;
        this.jobStatus = jobStatus;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.nextRunAt = nextRunAt;
        this.updatedAt = updatedAt;
        this.lastError = lastError;
    }

    public void markAsRunning() {
        this.jobStatus = JobStatus.RUNNING;
        this.updatedAt = Instant.now();
    }
    public void markAsCompleted() {
        this.jobStatus = JobStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }
    public void markAsFailed() {
        this.jobStatus = JobStatus.FAILED;
        this.updatedAt = Instant.now();
    }
    public void incrementRetryCount() {
        this.retryCount++;
    }
    public void setError(String error) {
        this.lastError = error;
    }
    public void scheduleRetry(Instant nextRunAt) {
        this.jobStatus = JobStatus.PENDING;
        this.nextRunAt = nextRunAt;
        this.updatedAt = Instant.now();
    }
}
