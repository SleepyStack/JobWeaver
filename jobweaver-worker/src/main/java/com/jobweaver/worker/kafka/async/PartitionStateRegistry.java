package com.jobweaver.worker.kafka.async;

import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PartitionStateRegistry {

    private final ConcurrentHashMap<TopicPartition, PartitionState> states =
            new ConcurrentHashMap<>();

    public PartitionState getOrCreate(TopicPartition topicPartition) {
        return states.computeIfAbsent(topicPartition, tp -> new PartitionState());
    }

    public PartitionState get(TopicPartition topicPartition) {
        return states.get(topicPartition);
    }

    public void remove(TopicPartition topicPartition) {
        PartitionState removed = states.remove(topicPartition);
        if (removed != null) {
            removed.reset();
        }
    }

    public void clear() {
        states.clear();
    }
}