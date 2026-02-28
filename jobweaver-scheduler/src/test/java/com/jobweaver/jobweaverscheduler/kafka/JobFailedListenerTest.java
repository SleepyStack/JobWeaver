package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobFailedEvent;
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
class JobFailedListenerTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private JobFailedListener listener;

    @Test
    @DisplayName("calls handleFailure with correct params and acknowledges")
    void happyPath() {
        UUID jobId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();
        JobFailedEvent event = new JobFailedEvent(jobId, "timeout error");

        listener.handleFailed(event, traceId, acknowledgment);

        verify(schedulerService).handleFailure(jobId, "timeout error");
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("does not acknowledge when service throws")
    void doesNotAckOnException() {
        UUID jobId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();
        JobFailedEvent event = new JobFailedEvent(jobId, "error");

        doThrow(new RuntimeException("service error"))
                .when(schedulerService).handleFailure(jobId, "error");

        assertThatThrownBy(() -> listener.handleFailed(event, traceId, acknowledgment))
                .isInstanceOf(RuntimeException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
