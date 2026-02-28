package com.jobweaver.jobweaverscheduler.repository;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
    @Query("""
   SELECT j
   FROM JobExecution j
   WHERE j.jobStatus = 'PENDING'
   AND j.nextRunAt <= :now
""")
    List<JobExecution> findReadyJobs(Instant now);
}
