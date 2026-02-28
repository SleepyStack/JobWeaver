package com.jobweaver.api.entity;

import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs",indexes = {@Index(name = "idx_jobs_trace_id", columnList = "traceId"),
                            @Index(name = "idx_jobs_created_at", columnList = "createdAt")}
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Getter
public class Job {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb",  nullable = false)
    private SimulationInstruction instruction;

    @Column(nullable = false)
    private String traceId;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public Job(JobType type, SimulationInstruction instruction, String traceId) {
        this.type = type;
        this.instruction = instruction;
        this.traceId = traceId;
    }
}
