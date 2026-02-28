package com.jobweaver.worker.kafka.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionStateTest {

    private PartitionState state;

    @BeforeEach
    void setUp() {
        state = new PartitionState();
    }

    @Nested
    @DisplayName("addInFlight and markCompleted")
    class BasicFlow {

        @Test
        @DisplayName("single offset in-flight then completed advances commit")
        void singleOffset() {
            state.addInFlight(0);
            state.markCompleted(0);

            Optional<Long> commit = state.tryAdvanceCommit();

            assertThat(commit).isPresent().hasValue(0L);
        }

        @Test
        @DisplayName("contiguous offsets advance commit to highest")
        void contiguousOffsets() {
            state.addInFlight(0);
            state.addInFlight(1);
            state.addInFlight(2);

            state.markCompleted(0);
            state.markCompleted(1);
            state.markCompleted(2);

            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(2L);
        }

        @Test
        @DisplayName("gap in completed offsets stops advancement")
        void gapStopsAdvancement() {
            state.addInFlight(0);
            state.addInFlight(1);
            state.addInFlight(2);

            state.markCompleted(0);
            // Skip 1
            state.markCompleted(2);

            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(0L);
        }

        @Test
        @DisplayName("filling gap allows full advancement")
        void fillingGapAllowsAdvancement() {
            state.addInFlight(0);
            state.addInFlight(1);
            state.addInFlight(2);

            state.markCompleted(0);
            state.markCompleted(2);

            // Advance to 0
            state.tryAdvanceCommit();

            // Now fill the gap
            state.markCompleted(1);

            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(2L);
        }
    }

    @Nested
    @DisplayName("tryAdvanceCommit")
    class TryAdvanceCommit {

        @Test
        @DisplayName("returns empty when nothing to advance")
        void emptyWhenNothingToAdvance() {
            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isEmpty();
        }

        @Test
        @DisplayName("returns empty when in-flight but not completed")
        void emptyWhenInFlightNotCompleted() {
            state.addInFlight(0);

            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isEmpty();
        }

        @Test
        @DisplayName("returns empty on second call without new completions")
        void emptyOnSecondCallWithoutNewCompletions() {
            state.addInFlight(0);
            state.markCompleted(0);

            state.tryAdvanceCommit(); // First call advances

            Optional<Long> secondCall = state.tryAdvanceCommit();
            assertThat(secondCall).isEmpty();
        }

        @Test
        @DisplayName("handles out-of-order completions correctly")
        void outOfOrderCompletions() {
            state.addInFlight(0);
            state.addInFlight(1);
            state.addInFlight(2);
            state.addInFlight(3);

            // Complete out of order: 3, 1, 0, 2
            state.markCompleted(3);
            state.markCompleted(1);
            state.markCompleted(0);

            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(1L); // 0 and 1 contiguous

            state.markCompleted(2);
            commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(3L); // Now all contiguous
        }
    }

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("clears all state")
        void clearsAllState() {
            state.addInFlight(0);
            state.addInFlight(1);
            state.markCompleted(0);

            state.reset();

            // After reset, tryAdvanceCommit should return empty
            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isEmpty();
        }

        @Test
        @DisplayName("can add new offsets after reset")
        void canAddAfterReset() {
            state.addInFlight(0);
            state.markCompleted(0);
            state.tryAdvanceCommit();

            state.reset();

            state.addInFlight(10);
            state.markCompleted(10);

            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(10L);
        }
    }

    @Nested
    @DisplayName("removeInFlight")
    class RemoveInFlight {

        @Test
        @DisplayName("removes offset from in-flight set")
        void removesOffset() {
            state.addInFlight(0);
            state.addInFlight(1);

            state.removeInFlight(0);
            state.markCompleted(0);

            // offset 0 was removed from in-flight and added to completed
            // Should still advance since it's in the completed set
            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue(0L);
        }
    }

    @Nested
    @DisplayName("Thread safety smoke test")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent add and complete does not throw")
        void concurrentAccess() throws InterruptedException {
            int numOffsets = 1000;

            // Add all in-flight
            Thread adder = new Thread(() -> {
                for (int i = 0; i < numOffsets; i++) {
                    state.addInFlight(i);
                }
            });

            Thread completer = new Thread(() -> {
                for (int i = 0; i < numOffsets; i++) {
                    state.markCompleted(i);
                }
            });

            adder.start();
            adder.join();
            completer.start();
            completer.join();

            // Should be able to advance to the last offset
            Optional<Long> commit = state.tryAdvanceCommit();
            assertThat(commit).isPresent().hasValue((long) (numOffsets - 1));
        }
    }
}
