package com.jobweaver.api.kafka;

import com.jobweaver.api.dto.JobSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobEventProducer {
    private final KafkaTemplate<String, JobSubmittedEvent> kafkaTemplate;

    public void sendMessage(UUID jobId, String traceId, String eventId){
        JobSubmittedEvent jobSubmittedEvent = new JobSubmittedEvent();
    }
}
