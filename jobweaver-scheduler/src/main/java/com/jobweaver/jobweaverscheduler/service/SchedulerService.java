package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.kafka.DeadLetterQueuePublisher;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final JobExecutionRepository jobExecutionRepository;
    private final DeadLetterQueuePublisher deadLetterQueuePublisher;

    @Transactional
    public void markCompleted(UUID jobId) {

        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow();

        if (job.getJobStatus() == JobStatus.COMPLETED) {
            return;
        }

        if (job.getJobStatus() != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Invalid state transition to COMPLETED from " + job.getJobStatus()
            );
        }

        job.markAsCompleted();
    }

    @Transactional
    public void handleFailure(UUID jobId, String error) {

        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow();

        if (job.getJobStatus() != JobStatus.RUNNING) {
            return;
        }

        job.incrementRetryCount();
        job.setError(error);

        if (job.getRetryCount() > job.getMaxRetries()) {

            job.markAsFailed();
            deadLetterQueuePublisher.publish(job);

        } else {

            long delaySeconds = computeBackoff(job.getRetryCount());

            job.scheduleRetry(Instant.now().plusSeconds(delaySeconds));
        }
    }
    private long computeBackoff(int retryCount) {
        long base = 5;
        long max = 300;

        long delay = base * (long) Math.pow(2, retryCount);

        return Math.min(delay, max);
    }
}
