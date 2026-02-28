package com.jobweaver.worker.exception;

import org.apache.kafka.common.TopicPartition;

/**
 * Thrown when a consumed Kafka record has missing or unparseable
 * headers (e.g. {@code traceId}, {@code eventId}).
 */
public class MalformedRecordException extends WorkerException {

    private final String headerName;
    private final TopicPartition topicPartition;
    private final long offset;

    public MalformedRecordException(
            String message,
            String headerName,
            TopicPartition topicPartition,
            long offset
    ) {
        super(message, ErrorCode.MALFORMED_RECORD);
        this.headerName = headerName;
        this.topicPartition = topicPartition;
        this.offset = offset;
    }

    public MalformedRecordException(
            String message,
            String headerName,
            TopicPartition topicPartition,
            long offset,
            Throwable cause
    ) {
        super(message, ErrorCode.MALFORMED_RECORD);
        initCause(cause);
        this.headerName = headerName;
        this.topicPartition = topicPartition;
        this.offset = offset;
    }

    public String getHeaderName() { return headerName; }

    public TopicPartition getTopicPartition() { return topicPartition; }

    public long getOffset() { return offset; }
}
