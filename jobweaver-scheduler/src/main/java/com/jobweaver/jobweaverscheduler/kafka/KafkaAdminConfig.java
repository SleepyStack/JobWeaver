package com.jobweaver.jobweaverscheduler.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaAdminConfig {

    @Value("${app.kafka.topics.run-job}")
    private String runJobTopic;

    @Value("${app.kafka.topics.job-dead-letter}")
    private String deadLetterTopic;

    @Bean
    public NewTopic runJobTopic() {
        return TopicBuilder
                .name(runJobTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder
                .name(deadLetterTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
