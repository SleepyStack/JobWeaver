package com.jobweaver.api.kafka;

import com.jobweaver.api.exceptions.EventPublishException;
import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobEventPublisherTest {

    @Mock
    private KafkaTemplate<String, JobCreatedEvent> kafkaTemplate;

    private JobEventPublisher publisher;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, JobCreatedEvent>> recordCaptor;

    private static final String TOPIC = "job-created";
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String TRACE_ID = UUID.randomUUID().toString();
    private static final String EVENT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        publisher = new JobEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "topic", TOPIC);
    }

    @Nested
    @DisplayName("publish - happy path")
    class HappyPath {

        @Test
        @DisplayName("sends producer record with correct topic and key")
        void sendsToCorrectTopicAndKey() throws Exception {
            JobCreatedEvent event = buildEvent();
            CompletableFuture future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            publisher.publish(event, TRACE_ID, EVENT_ID);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, JobCreatedEvent> record = recordCaptor.getValue();

            assertThat(record.topic()).isEqualTo(TOPIC);
            assertThat(record.key()).isEqualTo(JOB_ID.toString());
            assertThat(record.value()).isEqualTo(event);
        }

        @Test
        @DisplayName("adds traceId and eventId headers")
        void addsHeaders() throws Exception {
            JobCreatedEvent event = buildEvent();
            CompletableFuture future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            publisher.publish(event, TRACE_ID, EVENT_ID);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, JobCreatedEvent> record = recordCaptor.getValue();

            String traceIdHeader = new String(
                    record.headers().lastHeader("traceId").value(), StandardCharsets.UTF_8);
            String eventIdHeader = new String(
                    record.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8);

            assertThat(traceIdHeader).isEqualTo(TRACE_ID);
            assertThat(eventIdHeader).isEqualTo(EVENT_ID);
        }
    }

    @Nested
    @DisplayName("publish - error handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws EventPublishException when send fails")
        void throwsOnFailure() {
            JobCreatedEvent event = buildEvent();
            CompletableFuture future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Broker unreachable"));
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            assertThatThrownBy(() -> publisher.publish(event, TRACE_ID, EVENT_ID))
                    .isInstanceOf(EventPublishException.class)
                    .hasMessageContaining("Failed to publish event for job " + JOB_ID);
        }
    }

    private JobCreatedEvent buildEvent() {
        SimulationInstruction instruction = new SimulationInstruction(
                List.of(new LogStep("test"))
        );
        return new JobCreatedEvent(JOB_ID, JobType.SIMULATION, instruction, 3);
    }
}
