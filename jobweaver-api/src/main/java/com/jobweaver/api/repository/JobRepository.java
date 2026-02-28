package com.jobweaver.api.repository;

import com.jobweaver.api.entity.Job;
import com.jobweaver.common.messaging.enumeration.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByType(JobType type, Pageable pageable);
}
