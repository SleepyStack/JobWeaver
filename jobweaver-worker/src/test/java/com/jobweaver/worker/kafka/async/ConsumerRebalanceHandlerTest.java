package com.jobweaver.worker.kafka.async;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerRebalanceHandlerTest {

    @Mock
    private PartitionStateRegistry registry;

    @Mock
    private OffsetCommitCoordinator commitCoordinator;

    @Mock
    private Consumer<?, ?> consumer;

    @InjectMocks
    private ConsumerRebalanceHandler handler;

    @Nested
    @DisplayName("onPartitionsRevokedBeforeCommit")
    class OnPartitionsRevoked {

        @Test
        @DisplayName("flushes commits for each partition with existing state")
        void flushesCommitsForEachPartition() {
            TopicPartition tp0 = new TopicPartition("topic", 0);
            TopicPartition tp1 = new TopicPartition("topic", 1);
            PartitionState state0 = new PartitionState();
            PartitionState state1 = new PartitionState();

            when(registry.get(tp0)).thenReturn(state0);
            when(registry.get(tp1)).thenReturn(state1);

            Collection<TopicPartition> partitions = List.of(tp0, tp1);

            handler.onPartitionsRevokedBeforeCommit(consumer, partitions);

            verify(commitCoordinator).attemptCommit(tp0, consumer, state0);
            verify(commitCoordinator).attemptCommit(tp1, consumer, state1);
            verify(registry).remove(tp0);
            verify(registry).remove(tp1);
        }

        @Test
        @DisplayName("skips commit for partition without existing state")
        void skipsNullState() {
            TopicPartition tp = new TopicPartition("topic", 0);
            when(registry.get(tp)).thenReturn(null);

            handler.onPartitionsRevokedBeforeCommit(consumer, List.of(tp));

            verifyNoInteractions(commitCoordinator);
            verify(registry).remove(tp);
        }

        @Test
        @DisplayName("handles empty partition list")
        void emptyPartitionList() {
            handler.onPartitionsRevokedBeforeCommit(consumer, List.of());

            verifyNoInteractions(commitCoordinator);
            verify(registry, never()).remove(any());
        }
    }
}
