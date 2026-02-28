package com.jobweaver.worker.kafka.async;

import com.jobweaver.worker.exception.OffsetCommitException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Advances the committed offset for a partition to the highest
 * contiguous completed offset. <b>Must only be called from the
 * Kafka consumer thread</b> (the listener thread or a rebalance
 * callback) because {@code Consumer} is not thread-safe.
 */
@Slf4j
@Component
public class OffsetCommitCoordinator {

    public void attemptCommit(
            TopicPartition topicPartition,
            Consumer<?, ?> consumer,
            PartitionState state
    ) {

        Optional<Long> maybeOffset = state.tryAdvanceCommit();

        if (maybeOffset.isEmpty()) {
            return;
        }

        long commitOffset = maybeOffset.get() + 1;

        log.debug("Committing offset {} for {}", commitOffset, topicPartition);

        try {
            consumer.commitSync(
                    Map.of(
                            topicPartition,
                            new OffsetAndMetadata(commitOffset)
                    )
            );
        } catch (Exception ex) {
            throw new OffsetCommitException(
                    "Failed to commit offset " + commitOffset + " for " + topicPartition,
                    topicPartition, commitOffset, ex
            );
        }
    }
}