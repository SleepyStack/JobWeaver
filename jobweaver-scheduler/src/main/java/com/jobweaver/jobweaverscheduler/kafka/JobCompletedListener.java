package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobCompletedEvent;
import com.jobweaver.jobweaverscheduler.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobCompletedListener {

    private final SchedulerService schedulerService;

    @KafkaListener(
            topics = "${app.kafka.topics.job-completed}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCompleted(JobCompletedEvent event,
                                @Header("traceId") String traceId,
                                Acknowledgment ack) {

        try {
            MDC.put("traceId", traceId);

            schedulerService.markCompleted(event.jobId());

            ack.acknowledge();

        } finally {
            MDC.clear();
        }
    }
}
