package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RunJobPublisher {

    private final KafkaTemplate<String, RunJobEvent> kafkaTemplate;

    @Value("${app.kafka.topics.run-job}")
    private String topic;

    public void publishRunJob(UUID jobId, String traceId, SimulationInstruction instruction) {

        String eventId = UUID.randomUUID().toString();

        RunJobEvent event = new RunJobEvent(jobId,instruction);

        ProducerRecord<String, RunJobEvent> record =
                new ProducerRecord<>(topic, jobId.toString(), event);

        record.headers().add("traceId",
                traceId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventId",
                eventId.getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);
    }
}
