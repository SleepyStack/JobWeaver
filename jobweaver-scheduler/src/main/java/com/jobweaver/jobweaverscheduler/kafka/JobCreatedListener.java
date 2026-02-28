package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.jobweaverscheduler.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class JobCreatedListener {

    private final IngestionService ingestionService;

    @KafkaListener(
            topics = "${app.kafka.topics.job-created}",
            containerFactory = "jobCreatedListenerFactory"
    )
    public void handle(JobCreatedEvent event,
                       @Header("traceId") String traceId,
                       Acknowledgment ack) {

        try {
            MDC.put("traceId", traceId);

            ingestionService.persistIfNotExists(event, traceId);

            ack.acknowledge();

        } finally {
            MDC.clear();
        }
    }
}
