package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.exception.InvalidStateTransitionException;
import com.jobweaver.jobweaverscheduler.exception.JobNotFoundException;
import com.jobweaver.jobweaverscheduler.kafka.DeadLetterQueuePublisher;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final JobExecutionRepository jobExecutionRepository;
    private final DeadLetterQueuePublisher deadLetterQueuePublisher;

    @Transactional
    public void markCompleted(UUID jobId) {

        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getJobStatus() == JobStatus.COMPLETED) {
            log.debug("Job already completed, ignoring duplicate");
            return;
        }

        if (job.getJobStatus() != JobStatus.RUNNING) {
            throw new InvalidStateTransitionException(
                    job.getJobStatus(), JobStatus.COMPLETED, jobId, job.getTraceId());
        }

        job.markAsCompleted();
        log.info("Job marked COMPLETED");
    }

    @Transactional
    public void handleFailure(UUID jobId, String error) {

        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getJobStatus() != JobStatus.RUNNING) {
            log.debug("Job not in RUNNING state, ignoring failure event");
            return;
        }

        job.incrementRetryCount();
        job.setError(error);

        if (job.getRetryCount() > job.getMaxRetries()) {

            job.markAsFailed();
            log.info("Job exhausted retries ({}/{}), moved to FAILED — publishing to DLQ",
                    job.getRetryCount(), job.getMaxRetries());
            deadLetterQueuePublisher.publish(job);

        } else {

            long delaySeconds = computeBackoff(job.getRetryCount());
            job.scheduleRetry(Instant.now().plusSeconds(delaySeconds));
            log.info("Job scheduled for retry {}/{} in {}s",
                    job.getRetryCount(), job.getMaxRetries(), delaySeconds);
        }
    }

    private long computeBackoff(int retryCount) {
        long base = 5;
        long max = 300;

        long delay = base * (long) Math.pow(2, retryCount);

        return Math.min(delay, max);
    }
}
