package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.DeadLetterEvent;
import com.jobweaver.common.messaging.events.RunJobEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> producerConfigs() {

        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return config;
    }

    @Bean
    public ProducerFactory<String, RunJobEvent> runJobProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, RunJobEvent> runJobKafkaTemplate(
            ProducerFactory<String, RunJobEvent> runJobProducerFactory) {
        return new KafkaTemplate<>(runJobProducerFactory);
    }

    @Bean
    public ProducerFactory<String, DeadLetterEvent> deadLetterProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, DeadLetterEvent> deadLetterKafkaTemplate(
            ProducerFactory<String, DeadLetterEvent> deadLetterProducerFactory) {
        return new KafkaTemplate<>(deadLetterProducerFactory);
    }
}
