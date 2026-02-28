package com.jobweaver.worker.kafka.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerRebalanceHandler implements ConsumerAwareRebalanceListener {

    private final PartitionStateRegistry registry;
    private final OffsetCommitCoordinator commitCoordinator;

    /**
     * Called BEFORE the container commits offsets during a rebalance.
     * We flush any completed-but-uncommitted offsets so they are not
     * re-delivered to the new partition owner.
     */
    @Override
    public void onPartitionsRevokedBeforeCommit(
            Consumer<?, ?> consumer,
            Collection<TopicPartition> partitions) {

        for (TopicPartition tp : partitions) {
            PartitionState state = registry.get(tp);
            if (state != null) {
                log.info("Rebalance: flushing commits for {}", tp);
                commitCoordinator.attemptCommit(tp, consumer, state);
            }
        }

        partitions.forEach(registry::remove);
    }
}