package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final JobExecutionRepository jobExecutionRepository;

    @Transactional
    public void persistIfNotExists(JobCreatedEvent event, String traceId) {
        try {

            JobExecution execution = new JobExecution(
                    event.jobId(),
                    traceId,
                    event.instruction(),
                    JobStatus.PENDING,
                    0,
                    event.maxRetries(),
                    Instant.now(),
                    Instant.now(),
                    null);

            jobExecutionRepository.save(execution);
            log.info("Ingested job execution maxRetries={}", event.maxRetries());

        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate ingestion ignored for jobId={} traceId={}", event.jobId(), traceId);
        }
    }
}
