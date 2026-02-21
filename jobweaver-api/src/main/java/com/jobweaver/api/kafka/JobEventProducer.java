package com.jobweaver.api.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobEventProducer {
    private final KafkaTemplate<String, JobCreatedEvent> kafkaTemplate;

    public void sendMessage(UUID jobId){
        JobCreatedEvent event = new JobCreatedEvent(jobId);
        kafkaTemplate.send("job-execution-topic", event);
    }
}
