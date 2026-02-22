package com.jobweaver.worker.entity;

import com.jobweaver.common.model.JobStatus;
import com.jobweaver.common.model.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private int retryCount;
    private int maxRetries;

    private String workerId;
    private String lastError;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private boolean cancelRequested;

    public void markAsRunning(String workerId) {
        if (this.status != JobStatus.QUEUED)
            throw new IllegalStateException("Only QUEUED jobs can start");

        this.status = JobStatus.RUNNING;
        this.workerId = workerId;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsSuccess() {
        if (this.status != JobStatus.RUNNING)
            throw new IllegalStateException("Only RUNNING jobs can succeed");

        this.status = JobStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        if (this.status != JobStatus.RUNNING)
            throw new IllegalStateException("Only RUNNING jobs can fail");

        this.status = JobStatus.FAILED;
        this.retryCount++;
        this.lastError = error;
        this.completedAt = LocalDateTime.now();
    }
}
