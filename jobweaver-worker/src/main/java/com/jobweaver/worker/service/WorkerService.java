package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.worker.kafka.WorkerEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final ExecutionAttemptProcessor attemptProcessor;
    private final WorkerEventPublisher eventPublisher;

    public void process(UUID eventId, String traceId, RunJobEvent event) {

        log.info("Processing job eventId={}", eventId);

        ProcessingResult result = attemptProcessor.executeTransaction(eventId, traceId, event);

        if (result.duplicate()) {
            log.debug("Duplicate event {} — republishing outcome", eventId);
        }

        if (result.success()) {
            log.info("Job execution succeeded — publishing completion event");
            eventPublisher.publishSuccess(event.jobId(), traceId);
        } else if (result.failure()) {
            log.info("Job execution failed — publishing failure event error={}",
                    result.errorMessage());
            eventPublisher.publishFailure(
                    event.jobId(),
                    result.errorMessage(),
                    traceId);
        } else {
            log.debug("Duplicate running event {} — skipping", eventId);
        }
    }
}