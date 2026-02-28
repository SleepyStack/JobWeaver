package com.jobweaver.worker.entity;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "execution_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_execution_event_id", columnNames = "eventId")
        },
        indexes = {
                @Index(name = "idx_execution_job_id", columnList = "jobId"),
                @Index(name = "idx_execution_started_at", columnList = "startedAt")
        }
)
@NoArgsConstructor
@Getter
public class ExecutionAttempt {

    @Id
    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private String traceId;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private ExecutionOutcome outcome;

    @Column(length = 2000)
    private String errorMessage;

    public ExecutionAttempt(UUID eventId, UUID jobId, String traceId, Instant startedAt, ExecutionOutcome outcome, String errorMessage) {
        this.eventId = eventId;
        this.jobId = jobId;
        this.traceId = traceId;
        this.startedAt = startedAt;
        this.outcome = outcome;
        this.errorMessage = errorMessage;
    }
    public void markSuccess(){
        this.outcome = ExecutionOutcome.SUCCESS;
        this.finishedAt = Instant.now();
    }
    public void markFailure(String errorMessage){
        this.outcome = ExecutionOutcome.FAILURE;
        this.finishedAt = Instant.now();
        this.errorMessage = errorMessage;
    }
}
