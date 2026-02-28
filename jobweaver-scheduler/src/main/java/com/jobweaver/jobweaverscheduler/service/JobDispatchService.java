package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.kafka.RunJobPublisher;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobDispatchService {

    private final JobExecutionRepository repository;
    private final RunJobPublisher publisher;

    @Transactional
    public void dispatchPendingJobs() {

        List<JobExecution> jobs =
                repository.findReadyJobs(Instant.now());

        for (JobExecution job : jobs) {

            job.markAsRunning();

            publisher.publishRunJob(
                    job.getJobId(),
                    job.getTraceId(),
                    job.getInstruction()
            );
        }
    }
}
