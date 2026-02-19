package com.jobweaver.api.entity;

import com.jobweaver.common.model.JobStatus;
import com.jobweaver.common.model.JobType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Job {
    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private JobType type;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private int retryCount;
    private int maxRetryCount;

    private String workerId;
    private String lastError;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private boolean cancelRequested;

}
