package com.jobweaver.worker.kafka;

import com.jobweaver.common.messaging.events.JobCompletedEvent;
import com.jobweaver.common.messaging.events.JobFailedEvent;
import com.jobweaver.worker.exception.EventPublishException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkerEventPublisher {

    private static final String TOPIC_JOB_COMPLETED = "job-completed";
    private static final String TOPIC_JOB_FAILED = "job-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSuccess(UUID jobId, String traceId) {

        JobCompletedEvent event =
                new JobCompletedEvent(jobId);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(TOPIC_JOB_COMPLETED, jobId.toString(), event);

        record.headers().add("traceId",
                traceId.getBytes(StandardCharsets.UTF_8));

        record.headers().add("eventId",
                UUID.randomUUID().toString()
                        .getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).join();
        } catch (Exception ex) {
            throw new EventPublishException(
                    "Failed to publish job-completed for job " + jobId,
                    TOPIC_JOB_COMPLETED, jobId, traceId, ex
            );
        }
    }

    public void publishFailure(UUID jobId,
                               String errorMessage,
                               String traceId) {

        JobFailedEvent event =
                new JobFailedEvent(jobId, errorMessage);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(TOPIC_JOB_FAILED, jobId.toString(), event);

        record.headers().add("traceId",
                traceId.getBytes(StandardCharsets.UTF_8));

        record.headers().add("eventId",
                UUID.randomUUID().toString()
                        .getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).join();
        } catch (Exception ex) {
            throw new EventPublishException(
                    "Failed to publish job-failed for job " + jobId,
                    TOPIC_JOB_FAILED, jobId, traceId, ex
            );
        }
    }
}