package com.jobweaver.api.entity;

import com.jobweaver.api.exceptions.ApiException;
import com.jobweaver.api.exceptions.ErrorCode;
import com.jobweaver.common.model.JobStatus;
import com.jobweaver.common.model.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
@Getter
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

    private UUID traceId;

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

    public Job(JobType type, Map<String, Object> payload, JobStatus status, int maxRetryCount) {
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.maxRetries = maxRetryCount;
    }
}
