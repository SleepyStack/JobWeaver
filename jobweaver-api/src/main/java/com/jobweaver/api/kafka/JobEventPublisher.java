package com.jobweaver.api.kafka;

import com.jobweaver.common.messaging.events.JobCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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

        kafkaTemplate.send(record);
    }
}
