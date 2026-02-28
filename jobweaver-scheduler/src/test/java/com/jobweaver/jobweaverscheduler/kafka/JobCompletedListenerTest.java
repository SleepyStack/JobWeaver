package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobCompletedEvent;
import com.jobweaver.jobweaverscheduler.service.SchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCompletedListenerTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private JobCompletedListener listener;

    @Test
    @DisplayName("calls markCompleted and acknowledges")
    void happyPath() {
        UUID jobId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();
        JobCompletedEvent event = new JobCompletedEvent(jobId);

        listener.handleCompleted(event, traceId, acknowledgment);

        verify(schedulerService).markCompleted(jobId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("does not acknowledge when service throws")
    void doesNotAckOnException() {
        UUID jobId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();
        JobCompletedEvent event = new JobCompletedEvent(jobId);

        doThrow(new RuntimeException("service error"))
                .when(schedulerService).markCompleted(jobId);

        assertThatThrownBy(() -> listener.handleCompleted(event, traceId, acknowledgment))
                .isInstanceOf(RuntimeException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
