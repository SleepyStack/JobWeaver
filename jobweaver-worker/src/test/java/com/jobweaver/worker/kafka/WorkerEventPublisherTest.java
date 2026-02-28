package com.jobweaver.worker.kafka;

import com.jobweaver.common.messaging.events.JobCompletedEvent;
import com.jobweaver.common.messaging.events.JobFailedEvent;
import com.jobweaver.worker.exception.EventPublishException;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private WorkerEventPublisher publisher;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String TRACE_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        publisher = new WorkerEventPublisher(kafkaTemplate);
    }

    @Nested
    @DisplayName("publishSuccess")
    class PublishSuccess {

        @Test
        @DisplayName("sends JobCompletedEvent to job-completed topic")
        void sendsToCorrectTopic() {
            CompletableFuture future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            publisher.publishSuccess(JOB_ID, TRACE_ID);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, Object> record = recordCaptor.getValue();

            assertThat(record.topic()).isEqualTo("job-completed");
            assertThat(record.key()).isEqualTo(JOB_ID.toString());
            assertThat(record.value()).isInstanceOf(JobCompletedEvent.class);

            JobCompletedEvent event = (JobCompletedEvent) record.value();
            assertThat(event.jobId()).isEqualTo(JOB_ID);
        }

        @Test
        @DisplayName("adds traceId and eventId headers")
        void addsHeaders() {
            CompletableFuture future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            publisher.publishSuccess(JOB_ID, TRACE_ID);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, Object> record = recordCaptor.getValue();

            String traceId = new String(
                    record.headers().lastHeader("traceId").value(), StandardCharsets.UTF_8);
            assertThat(traceId).isEqualTo(TRACE_ID);
            assertThat(record.headers().lastHeader("eventId")).isNotNull();
        }

        @Test
        @DisplayName("throws EventPublishException on failure")
        void throwsOnFailure() {
            CompletableFuture future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("send failed"));
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            assertThatThrownBy(() -> publisher.publishSuccess(JOB_ID, TRACE_ID))
                    .isInstanceOf(EventPublishException.class)
                    .hasMessageContaining("job-completed");
        }
    }

    @Nested
    @DisplayName("publishFailure")
    class PublishFailure {

        @Test
        @DisplayName("sends JobFailedEvent to job-failed topic")
        void sendsToCorrectTopic() {
            CompletableFuture future = CompletableFuture.completedFuture(null);
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            publisher.publishFailure(JOB_ID, "error message", TRACE_ID);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, Object> record = recordCaptor.getValue();

            assertThat(record.topic()).isEqualTo("job-failed");
            assertThat(record.key()).isEqualTo(JOB_ID.toString());
            assertThat(record.value()).isInstanceOf(JobFailedEvent.class);

            JobFailedEvent event = (JobFailedEvent) record.value();
            assertThat(event.jobId()).isEqualTo(JOB_ID);
            assertThat(event.errorMessage()).isEqualTo("error message");
        }

        @Test
        @DisplayName("throws EventPublishException on failure")
        void throwsOnFailure() {
            CompletableFuture future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("send failed"));
            when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

            assertThatThrownBy(() -> publisher.publishFailure(JOB_ID, "error", TRACE_ID))
                    .isInstanceOf(EventPublishException.class)
                    .hasMessageContaining("job-failed");
        }
    }
}
