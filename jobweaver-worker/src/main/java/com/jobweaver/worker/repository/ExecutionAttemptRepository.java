package com.jobweaver.worker.repository;

import com.jobweaver.worker.entity.ExecutionAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExecutionAttemptRepository extends JpaRepository<ExecutionAttempt, UUID> {

    List<ExecutionAttempt> findByJobIdOrderByStartedAtDesc(UUID jobId);

    Page<ExecutionAttempt> findByJobId(UUID jobId, Pageable pageable);
}
