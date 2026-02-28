package com.jobweaver.worker.entity;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import jakarta.persistence.*;
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionOutcome outcome;

    @Column(length = 2000)
    private String errorMessage;
}
