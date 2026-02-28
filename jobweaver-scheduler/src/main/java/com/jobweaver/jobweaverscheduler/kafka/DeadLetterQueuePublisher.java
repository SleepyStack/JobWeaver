package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.DeadLetterEvent;
import com.jobweaver.jobweaverscheduler.entity.JobExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueuePublisher {

        private final KafkaTemplate<String, DeadLetterEvent> kafkaTemplate;

        @Value("${app.kafka.topics.job-dead-letter}")
        private String deadLetterTopic;

        public void publish(JobExecution job) {

                DeadLetterEvent event = new DeadLetterEvent(job.getJobId(), job.getLastError(), job.getRetryCount(),
                                Instant.now());

                ProducerRecord<String, DeadLetterEvent> record = new ProducerRecord<>(
                                deadLetterTopic,
                                job.getJobId().toString(),
                                event);

                record.headers().add("traceId",
                                job.getTraceId().getBytes(StandardCharsets.UTF_8));

                // Capture MDC so the async callback logs carry trace context
                Map<String, String> mdcContext = MDC.getCopyOfContextMap();

                kafkaTemplate.send(record).whenComplete((result, ex) -> {
                        if (mdcContext != null)
                                MDC.setContextMap(mdcContext);
                        try {
                                if (ex != null) {
                                        log.error("Failed to publish DeadLetterEvent to topic={}: {}",
                                                        deadLetterTopic, ex.getMessage(), ex);
                                } else {
                                        log.debug("Published DeadLetterEvent to topic={} offset={}",
                                                        deadLetterTopic, result.getRecordMetadata().offset());
                                }
                        } finally {
                                MDC.clear();
                        }
                });
        }
}
