package com.jobweaver.worker.kafka;

import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.worker.exception.MalformedRecordException;
import com.jobweaver.worker.kafka.async.OffsetCommitCoordinator;
import com.jobweaver.worker.kafka.async.PartitionState;
import com.jobweaver.worker.kafka.async.PartitionStateRegistry;
import com.jobweaver.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunJobListener {

    private final WorkerService workerService;
    private final ExecutorService jobProcessorExecutor;
    private final PartitionStateRegistry registry;
    private final OffsetCommitCoordinator commitCoordinator;

    @KafkaListener(topics = "run-job", containerFactory = "runJobKafkaListenerContainerFactory")
    public void listen(
            ConsumerRecord<String, RunJobEvent> record,
            Consumer<?, ?> consumer) {
        TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

        PartitionState state = registry.getOrCreate(topicPartition);

        // Commit any previously completed offsets — this runs on the
        // consumer thread, which is the only thread safe to call commitSync.
        commitCoordinator.attemptCommit(topicPartition, consumer, state);

        String traceId = extractRequiredHeader(record, "traceId", topicPartition);

        UUID eventId;
        try {
            eventId = UUID.fromString(
                    extractRequiredHeader(record, "eventId", topicPartition));
        } catch (IllegalArgumentException ex) {
            throw new MalformedRecordException(
                    "Header 'eventId' is not a valid UUID",
                    "eventId", topicPartition, record.offset(), ex);
        }

        long offset = record.offset();
        UUID jobId = record.value().jobId();

        // Set MDC before copying — jobId + traceId propagate to the async thread
        MDC.put("traceId", traceId);
        MDC.put("jobId", jobId.toString());
        log.info("Received RunJobEvent eventId={} offset={}", eventId, offset);

        state.addInFlight(offset);

        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // CallerRunsPolicy guarantees this never throws RejectedExecutionException;
        // if the pool is saturated the listener thread executes the task itself,
        // which naturally back-pressures poll().
        jobProcessorExecutor.submit(() -> {

            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }

            try {
                workerService.process(
                        eventId,
                        traceId,
                        record.value());
            } catch (Exception ex) {
                log.error(
                        "Unhandled error processing offset {} on {}: {}",
                        offset, topicPartition, ex.getMessage(), ex);
            } finally {
                // Always mark completed so the commit watermark can advance.
                // The job outcome (success/failure) is already persisted in
                // the DB and the downstream event has been published.
                state.markCompleted(offset);
                MDC.clear();
            }
        });
    }

    private String extractRequiredHeader(
            ConsumerRecord<?, ?> record,
            String headerName,
            TopicPartition topicPartition) {
        Header header = record.headers().lastHeader(headerName);

        if (header == null || header.value() == null) {
            throw new MalformedRecordException(
                    "Missing required header '" + headerName + "'",
                    headerName, topicPartition, record.offset());
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}