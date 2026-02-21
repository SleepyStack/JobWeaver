package com.jobweaver.api.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaAdminConfig {
    @Bean
    public NewTopic jobExecutionTopic() {
        return TopicBuilder
                .name("job-execution-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public KafkaAdmin kafkaAdmin() {

        Map<String, Object> configs = new HashMap<>();

        configs.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        return new KafkaAdmin(configs);
    }
}
