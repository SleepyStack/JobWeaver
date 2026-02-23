package com.jobweaver.worker.service;

import com.jobweaver.worker.kafka.JobCreatedEvent;
import com.jobweaver.worker.processing.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final ExecutorService executor;
    private final JobProcessor jobProcessor;

    public WorkerService(@Qualifier("jobProcessorExecutor") ExecutorService executor,
                         JobProcessor jobProcessor) {
        this.executor = executor;
        this.jobProcessor = jobProcessor;
    }

    /**
     * Kafka listener — one thread per partition (3 partitions).
     * Each message is immediately dispatched to the shared thread pool
     * (12 threads = 4 per consumer) so the consumer can keep polling.
     */
    @KafkaListener(
            topics = "job-execution-topic",
            groupId = "worker-group"
    )
    public void listen(JobCreatedEvent event) {
        log.info("Received job event: jobId={} on thread {}",
                event.getJobId(), Thread.currentThread().getName());

        executor.submit(() -> {
            try {
                jobProcessor.process(event.getJobId());
            } catch (Exception ex) {
                log.error("Unhandled error processing job {}: {}",
                        event.getJobId(), ex.getMessage(), ex);
            }
        });
    }
}
