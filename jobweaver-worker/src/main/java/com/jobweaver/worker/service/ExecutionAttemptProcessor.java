package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.enumeration.ExecutionOutcome;
import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.worker.entity.ExecutionAttempt;
import com.jobweaver.worker.repository.ExecutionAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Separated from WorkerService so that Spring's AOP proxy
 * can intercept the @Transactional method (self-invocation
 * within the same bean bypasses the proxy).
 */
@Service
@RequiredArgsConstructor
public class ExecutionAttemptProcessor {

    private final ExecutionAttemptRepository repository;
    private final SimulationExecutor executor;

    @Transactional
    public ProcessingResult executeTransaction(
            UUID eventId,
            String traceId,
            RunJobEvent event
    ) {

        Optional<ExecutionAttempt> existing =
                repository.findById(eventId);

        if (existing.isPresent()) {

            ExecutionAttempt attempt = existing.get();

            if (attempt.getOutcome() == ExecutionOutcome.RUNNING) {
                return ProcessingResult.ofDuplicateRunning();
            }

            if (attempt.getOutcome() == ExecutionOutcome.SUCCESS) {
                return ProcessingResult.ofDuplicateSuccess();
            }

            return ProcessingResult.ofDuplicateFailure(
                    attempt.getErrorMessage()
            );
        }

        ExecutionAttempt attempt = new ExecutionAttempt(
                eventId,
                event.jobId(),
                traceId,
                Instant.now(),
                ExecutionOutcome.RUNNING,
                null
        );

        repository.save(attempt);

        try {

            executor.execute(event.instruction(), event.jobId(), traceId);

            attempt.markSuccess();

            return ProcessingResult.ofSuccess();

        } catch (Exception ex) {

            attempt.markFailure(ex.getMessage());

            return ProcessingResult.ofFailure(ex.getMessage());
        }
    }
}
