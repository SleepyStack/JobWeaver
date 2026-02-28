package com.jobweaver.api.exceptions;

import java.util.UUID;

/**
 * Thrown when publishing a {@code JobCreatedEvent} to Kafka fails.
 * Wraps the underlying Kafka or serialization error.
 */
public class EventPublishException extends ApiException {

    private final String topic;

    public EventPublishException(String message, String topic, UUID jobId, Throwable cause) {
        super(message, ErrorCode.EVENT_PUBLISH_FAILED, jobId);
        initCause(cause);
        this.topic = topic;
    }

    public EventPublishException(String message, String topic, UUID jobId) {
        super(message, ErrorCode.EVENT_PUBLISH_FAILED, jobId);
        this.topic = topic;
    }

    public String getTopic() { return topic; }
}
