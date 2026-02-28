package com.jobweaver.jobweaverscheduler.kafka;

import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.jobweaverscheduler.service.IngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCreatedListenerTest {

    @Mock
    private IngestionService ingestionService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private JobCreatedListener listener;

    @Test
    @DisplayName("handles event, calls ingestion, and acknowledges")
    void happyPath() {
        UUID jobId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();
        SimulationInstruction instruction = new SimulationInstruction(List.of(new LogStep("test")));
        JobCreatedEvent event = new JobCreatedEvent(jobId, JobType.SIMULATION, instruction, 3);

        listener.handle(event, traceId, acknowledgment);

        verify(ingestionService).persistIfNotExists(event, traceId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("still clears MDC even when ingestion throws")
    void clearsMdcOnException() {
        UUID jobId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();
        SimulationInstruction instruction = new SimulationInstruction(List.of(new LogStep("test")));
        JobCreatedEvent event = new JobCreatedEvent(jobId, JobType.SIMULATION, instruction, 3);

        doThrow(new RuntimeException("DB error"))
                .when(ingestionService).persistIfNotExists(event, traceId);

        assertThatThrownBy(() -> listener.handle(event, traceId, acknowledgment))
                .isInstanceOf(RuntimeException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
