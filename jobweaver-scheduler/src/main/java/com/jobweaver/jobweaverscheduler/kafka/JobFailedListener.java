package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobFailedEvent;
import com.jobweaver.jobweaverscheduler.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobFailedListener {

    private final SchedulerService schedulerService;

    @KafkaListener(topics = "${app.kafka.topics.job-failed}", containerFactory = "jobFailedListenerFactory")
    public void handleFailed(JobFailedEvent event,
            @Header("traceId") String traceId,
            Acknowledgment ack) {

        try {
            MDC.put("traceId", traceId);
            MDC.put("jobId", event.jobId().toString());
            log.info("Received JobFailedEvent error={}", event.errorMessage());

            schedulerService.handleFailure(
                    event.jobId(),
                    event.errorMessage());

            ack.acknowledge();

        } finally {
            MDC.clear();
        }
    }
}
