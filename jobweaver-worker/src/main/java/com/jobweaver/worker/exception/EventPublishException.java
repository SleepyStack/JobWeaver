package com.jobweaver.worker.exception;

import java.util.UUID;

/**
 * Thrown when publishing a result event (job-completed / job-failed) to
 * Kafka fails.  Wraps the underlying Kafka or serialization error.
 */
public class EventPublishException extends WorkerException {

    private final String topic;

    public EventPublishException(String message, String topic, UUID jobId, String traceId, Throwable cause) {
        super(message, ErrorCode.EVENT_PUBLISH_FAILED, jobId, traceId, cause);
        this.topic = topic;
    }

    public String getTopic() { return topic; }
}
