package com.jobweaver.worker.kafka.async;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.TreeSet;

@Slf4j
public class PartitionState {

    private static final int MAX_PENDING_COMPLETED = 10_000;

    private Long lastCommittedOffset = null;

    private final TreeSet<Long> inFlight = new TreeSet<>();
    private final TreeSet<Long> completed = new TreeSet<>();

    public synchronized void addInFlight(long offset) {

        if (lastCommittedOffset == null) {
            lastCommittedOffset = offset - 1;
        }

        inFlight.add(offset);
    }

    public synchronized void markCompleted(long offset) {
        inFlight.remove(offset);
        completed.add(offset);

        if (completed.size() > MAX_PENDING_COMPLETED) {
            log.warn(
                    "Completed set has {} entries — possible stuck in-flight offset. "
                    + "Smallest in-flight: {}, lastCommitted: {}",
                    completed.size(),
                    inFlight.isEmpty() ? "none" : inFlight.first(),
                    lastCommittedOffset
            );
        }
    }

    public synchronized void removeInFlight(long offset) {
        inFlight.remove(offset);
    }

    public synchronized Optional<Long> tryAdvanceCommit() {

        if (lastCommittedOffset == null) {
            return Optional.empty();
        }

        long nextExpected = lastCommittedOffset + 1;
        boolean advanced = false;

        while (completed.contains(nextExpected)) {
            completed.remove(nextExpected);
            lastCommittedOffset = nextExpected;
            nextExpected++;
            advanced = true;
        }

        return advanced
                ? Optional.of(lastCommittedOffset)
                : Optional.empty();
    }

    public synchronized void reset() {
        inFlight.clear();
        completed.clear();
        lastCommittedOffset = null;
    }
}