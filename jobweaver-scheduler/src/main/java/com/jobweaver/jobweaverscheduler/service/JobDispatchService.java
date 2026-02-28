package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.kafka.RunJobPublisher;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDispatchService {

    private final JobExecutionRepository repository;
    private final RunJobPublisher publisher;

    @Transactional
    public void dispatchPendingJobs() {

        List<JobExecution> jobs = repository.findReadyJobs(Instant.now());

        if (!jobs.isEmpty()) {
            log.info("Dispatching {} pending job(s)", jobs.size());
        }

        for (JobExecution job : jobs) {
            try {
                MDC.put("traceId", job.getTraceId());
                MDC.put("jobId", job.getJobId().toString());

                job.markAsRunning();

                publisher.publishRunJob(
                        job.getJobId(),
                        job.getTraceId(),
                        job.getInstruction());

                log.info("Dispatched job to worker");

            } catch (Exception ex) {
                log.error("Failed to dispatch job: {}",
                        ex.getMessage(), ex);
            } finally {
                MDC.remove("traceId");
                MDC.remove("jobId");
            }
        }
    }
}
