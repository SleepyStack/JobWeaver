package com.jobweaver.worker.repository;

import com.jobweaver.worker.entity.ExecutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionAttemptRepository extends JpaRepository<ExecutionAttempt, UUID> {
}
