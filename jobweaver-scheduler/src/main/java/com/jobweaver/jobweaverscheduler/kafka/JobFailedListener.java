package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobFailedEvent;
import com.jobweaver.jobweaverscheduler.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobFailedListener {

    private final SchedulerService schedulerService;

    @KafkaListener(
            topics = "${app.kafka.topics.job-failed}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleFailed(JobFailedEvent event,
                             @Header("traceId") String traceId,
                             Acknowledgment ack) {

        try {
            MDC.put("traceId", traceId);

            schedulerService.handleFailure(
                    event.jobId(),
                    event.errorMessage()
            );

            ack.acknowledge();

        } finally {
            MDC.clear();
        }
    }
}
