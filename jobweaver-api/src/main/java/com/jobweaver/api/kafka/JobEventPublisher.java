package com.jobweaver.api.kafka;

import com.jobweaver.api.exceptions.EventPublishException;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventPublisher {

    private static final String TRACE_ID = "traceId";
    private static final String EVENT_ID = "eventId";

    private final KafkaTemplate<String, JobCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.job-created}")
    private String topic;

    public void publish(JobCreatedEvent event,
                        String traceId,
                        String eventId) {

        ProducerRecord<String, JobCreatedEvent> record =
                new ProducerRecord<>(topic, event.jobId().toString(), event);

        record.headers().add(
                TRACE_ID,
                traceId.getBytes(StandardCharsets.UTF_8)
        );

        record.headers().add(
                EVENT_ID,
                eventId.getBytes(StandardCharsets.UTF_8)
        );

        try {
            kafkaTemplate.send(record).get();
        } catch (Exception ex) {
            log.error("Failed to publish JobCreatedEvent to topic={} jobId={}", topic, event.jobId(), ex);
            throw new EventPublishException(
                    "Failed to publish event for job " + event.jobId(),
                    topic, event.jobId(), ex);
        }
    }
}
