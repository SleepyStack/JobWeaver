package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobCompletedEvent;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.common.messaging.events.JobFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "scheduler-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return config;
    }

    private <T> ConsumerFactory<String, T> typedConsumerFactory(Class<T> targetType) {
        JacksonJsonDeserializer<T> deserializer = new JacksonJsonDeserializer<>(targetType);
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseConsumerConfig(), new StringDeserializer(), deserializer);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> typedFactory(Class<T> targetType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(typedConsumerFactory(targetType));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JobCreatedEvent> jobCreatedListenerFactory() {
        return typedFactory(JobCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JobCompletedEvent> jobCompletedListenerFactory() {
        return typedFactory(JobCompletedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JobFailedEvent> jobFailedListenerFactory() {
        return typedFactory(JobFailedEvent.class);
    }
}
