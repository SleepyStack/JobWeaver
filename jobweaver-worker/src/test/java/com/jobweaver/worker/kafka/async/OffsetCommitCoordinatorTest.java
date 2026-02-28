package com.jobweaver.worker.kafka.async;

import com.jobweaver.worker.exception.OffsetCommitException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetCommitCoordinatorTest {

    @Mock
    private Consumer<?, ?> consumer;

    @Captor
    private ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> commitCaptor;

    private OffsetCommitCoordinator coordinator;
    private TopicPartition topicPartition;
    private PartitionState state;

    @BeforeEach
    void setUp() {
        coordinator = new OffsetCommitCoordinator();
        topicPartition = new TopicPartition("test-topic", 0);
        state = new PartitionState();
    }

    @Nested
    @DisplayName("attemptCommit")
    class AttemptCommit {

        @Test
        @DisplayName("commits offset + 1 when advance is possible")
        void commitsOffsetPlusOne() {
            state.addInFlight(5);
            state.markCompleted(5);

            coordinator.attemptCommit(topicPartition, consumer, state);

            verify(consumer).commitSync(commitCaptor.capture());
            Map<TopicPartition, OffsetAndMetadata> committed = commitCaptor.getValue();

            assertThat(committed).containsKey(topicPartition);
            // offset 5 completed, so commit offset 6 (next to be read)
            assertThat(committed.get(topicPartition).offset()).isEqualTo(6);
        }

        @Test
        @DisplayName("does not commit when nothing to advance")
        void doesNotCommitWhenNothingToAdvance() {
            state.addInFlight(0);
            // Not completed yet

            coordinator.attemptCommit(topicPartition, consumer, state);

            verifyNoInteractions(consumer);
        }

        @Test
        @DisplayName("does not commit on empty state")
        void doesNotCommitOnEmptyState() {
            coordinator.attemptCommit(topicPartition, consumer, state);
            verifyNoInteractions(consumer);
        }

        @Test
        @DisplayName("commits contiguous offsets correctly")
        void commitsContiguous() {
            state.addInFlight(0);
            state.addInFlight(1);
            state.addInFlight(2);

            state.markCompleted(0);
            state.markCompleted(1);
            state.markCompleted(2);

            coordinator.attemptCommit(topicPartition, consumer, state);

            verify(consumer).commitSync(commitCaptor.capture());
            assertThat(commitCaptor.getValue().get(topicPartition).offset()).isEqualTo(3);
        }

        @Test
        @DisplayName("throws OffsetCommitException when consumer.commitSync fails")
        void throwsOnCommitFailure() {
            state.addInFlight(0);
            state.markCompleted(0);

            doThrow(new RuntimeException("Commit failed"))
                    .when(consumer).commitSync(any(Map.class));

            assertThatThrownBy(() ->
                    coordinator.attemptCommit(topicPartition, consumer, state))
                    .isInstanceOf(OffsetCommitException.class)
                    .hasMessageContaining("Failed to commit offset");
        }
    }
}
