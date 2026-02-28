package com.jobweaver.jobweaverscheduler.exception;

import java.util.UUID;

/**
 * Thrown when publishing an event (run-job or dead-letter) to Kafka fails.
 * Wraps the underlying Kafka or serialization error.
 */
public class EventPublishException extends SchedulerException {

    private final String topic;

    public EventPublishException(String message, String topic, UUID jobId, String traceId, Throwable cause) {
        super(message, ErrorCode.EVENT_PUBLISH_FAILED, jobId, traceId, cause);
        this.topic = topic;
    }

    public EventPublishException(String message, String topic, UUID jobId, String traceId) {
        super(message, ErrorCode.EVENT_PUBLISH_FAILED, jobId, traceId);
        this.topic = topic;
    }

    public String getTopic() { return topic; }
}
