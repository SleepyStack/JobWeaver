package com.jobweaver.jobweaverscheduler.repository;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

  @Query(value = """
      SELECT * FROM job_executions
      WHERE job_status = 'PENDING'
        AND next_run_at <= :now
      ORDER BY next_run_at
      LIMIT 50
      FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<JobExecution> findReadyJobs(@Param("now") Instant now);

  List<JobExecution> findByJobStatus(JobStatus jobStatus);

  Page<JobExecution> findByJobStatus(JobStatus jobStatus, Pageable pageable);
}
