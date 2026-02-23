package com.jobweaver.worker.processing;

import com.jobweaver.worker.entity.Job;
import com.jobweaver.worker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Handles the full lifecycle of a single job:
 *   1. Load from DB
 *   2. Mark as RUNNING
 *   3. Decode & execute instructions
 *   4. Mark as SUCCESS or FAILED
 *
 * Designed to run on pool threads — one instance per job invocation.
 */
@Component
@RequiredArgsConstructor
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private final JobRepository jobRepository;
    private final InstructionExecutor instructionExecutor;
    private final TransactionTemplate txTemplate;

    /**
     * Process a job end-to-end. Safe to call from any thread.
     */
    public void process(UUID jobId) {
        String threadName = Thread.currentThread().getName();
        String label = jobId.toString().substring(0, 8) + "@" + threadName;

        log.info("[{}] Picking up job", label);

        Job job = claimJob(jobId, threadName, label);
        if (job == null) return; // already logged

        try {
            instructionExecutor.execute(job.getPayload(), label);
            completeJob(jobId, label);
        } catch (Exception ex) {
            failJob(jobId, ex.getMessage(), label);
        }
    }


    private Job claimJob(UUID jobId, String workerId, String label) {
        return txTemplate.execute(status -> {
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.error("[{}] Job not found in DB — skipping", label);
                return null;
            }
            try {
                job.markAsRunning(workerId);
                jobRepository.save(job);
                log.info("[{}] Job claimed — status=RUNNING, type={}", label, job.getType());
                return job;
            } catch (IllegalStateException e) {
                log.warn("[{}] Cannot claim job: {}", label, e.getMessage());
                return null;
            }
        });
    }

    private void completeJob(UUID jobId, String label) {
        txTemplate.executeWithoutResult(status -> {
            Job job = jobRepository.findById(jobId).orElseThrow();
            job.markAsSuccess();
            jobRepository.save(job);
            log.info("[{}] Job completed — status=SUCCESS", label);
        });
    }

    private void failJob(UUID jobId, String error, String label) {
        txTemplate.executeWithoutResult(status -> {
            Job job = jobRepository.findById(jobId).orElseThrow();
            job.markAsFailed(error);
            jobRepository.save(job);
            log.error("[{}] Job failed — status=FAILED, error={}", label, error);
        });
    }
}
