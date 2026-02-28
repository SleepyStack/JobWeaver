package com.jobweaver.worker.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaAdminConfig {

    @Value("${app.kafka.topics.job-completed}")
    private String jobCompletedTopic;

    @Value("${app.kafka.topics.job-failed}")
    private String jobFailedTopic;

    @Bean
    public NewTopic jobCompletedTopic() {
        return TopicBuilder
                .name(jobCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobFailedTopic() {
        return TopicBuilder
                .name(jobFailedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
