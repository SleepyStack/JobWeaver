package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunJobPublisher {

    private final KafkaTemplate<String, RunJobEvent> kafkaTemplate;

    @Value("${app.kafka.topics.run-job}")
    private String topic;

    public void publishRunJob(UUID jobId, String traceId, SimulationInstruction instruction) {

        String eventId = UUID.randomUUID().toString();

        RunJobEvent event = new RunJobEvent(jobId, instruction);

        ProducerRecord<String, RunJobEvent> record = new ProducerRecord<>(topic, jobId.toString(), event);

        record.headers().add("traceId",
                traceId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventId",
                eventId.getBytes(StandardCharsets.UTF_8));

        // Capture MDC so the async callback logs carry trace context
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (mdcContext != null)
                MDC.setContextMap(mdcContext);
            try {
                if (ex != null) {
                    log.error("Failed to publish RunJobEvent to topic={}: {}",
                            topic, ex.getMessage(), ex);
                } else {
                    log.debug("Published RunJobEvent to topic={} offset={}",
                            topic, result.getRecordMetadata().offset());
                }
            } finally {
                MDC.clear();
            }
        });
    }
}
