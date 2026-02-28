package com.jobweaver.worker.kafka.async;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionStateRegistryTest {

    private PartitionStateRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PartitionStateRegistry();
    }

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("creates new state for unknown partition")
        void createsNew() {
            TopicPartition tp = new TopicPartition("test-topic", 0);

            PartitionState state = registry.getOrCreate(tp);

            assertThat(state).isNotNull();
        }

        @Test
        @DisplayName("returns same instance for same partition")
        void returnsSame() {
            TopicPartition tp = new TopicPartition("test-topic", 0);

            PartitionState first = registry.getOrCreate(tp);
            PartitionState second = registry.getOrCreate(tp);

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("returns different instances for different partitions")
        void returnsDifferent() {
            TopicPartition tp0 = new TopicPartition("test-topic", 0);
            TopicPartition tp1 = new TopicPartition("test-topic", 1);

            PartitionState state0 = registry.getOrCreate(tp0);
            PartitionState state1 = registry.getOrCreate(tp1);

            assertThat(state0).isNotSameAs(state1);
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns null for unknown partition")
        void returnsNull() {
            TopicPartition tp = new TopicPartition("test-topic", 0);
            assertThat(registry.get(tp)).isNull();
        }

        @Test
        @DisplayName("returns state for known partition")
        void returnsState() {
            TopicPartition tp = new TopicPartition("test-topic", 0);
            PartitionState created = registry.getOrCreate(tp);

            assertThat(registry.get(tp)).isSameAs(created);
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("removes partition and resets state")
        void removesAndResets() {
            TopicPartition tp = new TopicPartition("test-topic", 0);
            PartitionState state = registry.getOrCreate(tp);
            state.addInFlight(0);

            registry.remove(tp);

            assertThat(registry.get(tp)).isNull();
        }

        @Test
        @DisplayName("does not throw for unknown partition")
        void noThrowForUnknown() {
            TopicPartition tp = new TopicPartition("test-topic", 99);
            registry.remove(tp); // Should not throw
        }

        @Test
        @DisplayName("getOrCreate creates new state after remove")
        void createsNewAfterRemove() {
            TopicPartition tp = new TopicPartition("test-topic", 0);
            PartitionState original = registry.getOrCreate(tp);
            original.addInFlight(5);

            registry.remove(tp);

            PartitionState newState = registry.getOrCreate(tp);
            assertThat(newState).isNotSameAs(original);
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("removes all partitions")
        void removesAll() {
            registry.getOrCreate(new TopicPartition("t", 0));
            registry.getOrCreate(new TopicPartition("t", 1));
            registry.getOrCreate(new TopicPartition("t", 2));

            registry.clear();

            assertThat(registry.get(new TopicPartition("t", 0))).isNull();
            assertThat(registry.get(new TopicPartition("t", 1))).isNull();
            assertThat(registry.get(new TopicPartition("t", 2))).isNull();
        }
    }
}
