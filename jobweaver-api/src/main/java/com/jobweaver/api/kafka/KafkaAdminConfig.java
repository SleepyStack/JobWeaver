package com.jobweaver.api.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaAdminConfig {

    @Value("${app.kafka.topics.job-created}")
    private String jobCreatedTopic;

    @Bean
    public NewTopic jobCreatedTopic() {
        return TopicBuilder
                .name(jobCreatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
