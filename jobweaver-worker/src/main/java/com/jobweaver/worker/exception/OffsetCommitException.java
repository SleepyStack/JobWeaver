package com.jobweaver.worker.exception;

import org.apache.kafka.common.TopicPartition;

/**
 * Thrown when a synchronous offset commit to Kafka fails.
 * Carries the partition and the offset that was being committed.
 */
public class OffsetCommitException extends WorkerException {

    private final TopicPartition topicPartition;
    private final long offset;

    public OffsetCommitException(
            String message,
            TopicPartition topicPartition,
            long offset,
            Throwable cause
    ) {
        super(message, ErrorCode.OFFSET_COMMIT_FAILED);
        initCause(cause);
        this.topicPartition = topicPartition;
        this.offset = offset;
    }

    public TopicPartition getTopicPartition() { return topicPartition; }

    public long getOffset() { return offset; }
}
