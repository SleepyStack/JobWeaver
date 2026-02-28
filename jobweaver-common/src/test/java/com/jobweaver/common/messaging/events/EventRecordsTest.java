package com.jobweaver.common.messaging.events;

import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.common.messaging.simulation.SleepStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventRecordsTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final SimulationInstruction INSTRUCTION =
            new SimulationInstruction(List.of(new SleepStep(100), new LogStep("test")));

    @Test
    @DisplayName("JobCreatedEvent stores all fields correctly")
    void jobCreatedEvent() {
        JobCreatedEvent event = new JobCreatedEvent(JOB_ID, JobType.SIMULATION, INSTRUCTION, 3);

        assertThat(event.jobId()).isEqualTo(JOB_ID);
        assertThat(event.type()).isEqualTo(JobType.SIMULATION);
        assertThat(event.instruction()).isEqualTo(INSTRUCTION);
        assertThat(event.maxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("RunJobEvent stores jobId and instruction")
    void runJobEvent() {
        RunJobEvent event = new RunJobEvent(JOB_ID, INSTRUCTION);

        assertThat(event.jobId()).isEqualTo(JOB_ID);
        assertThat(event.instruction()).isEqualTo(INSTRUCTION);
    }

    @Test
    @DisplayName("JobCompletedEvent stores jobId")
    void jobCompletedEvent() {
        JobCompletedEvent event = new JobCompletedEvent(JOB_ID);
        assertThat(event.jobId()).isEqualTo(JOB_ID);
    }

    @Test
    @DisplayName("JobFailedEvent stores jobId and errorMessage")
    void jobFailedEvent() {
        JobFailedEvent event = new JobFailedEvent(JOB_ID, "something went wrong");

        assertThat(event.jobId()).isEqualTo(JOB_ID);
        assertThat(event.errorMessage()).isEqualTo("something went wrong");
    }

    @Test
    @DisplayName("DeadLetterEvent stores all fields correctly")
    void deadLetterEvent() {
        Instant now = Instant.now();
        DeadLetterEvent event = new DeadLetterEvent(JOB_ID, "max retries exhausted", 5, now);

        assertThat(event.jobId()).isEqualTo(JOB_ID);
        assertThat(event.reason()).isEqualTo("max retries exhausted");
        assertThat(event.finalRetryCount()).isEqualTo(5);
        assertThat(event.failedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Records with same values are equal")
    void recordEquality() {
        JobCompletedEvent a = new JobCompletedEvent(JOB_ID);
        JobCompletedEvent b = new JobCompletedEvent(JOB_ID);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Records with different values are not equal")
    void recordInequality() {
        JobCompletedEvent a = new JobCompletedEvent(UUID.randomUUID());
        JobCompletedEvent b = new JobCompletedEvent(UUID.randomUUID());
        assertThat(a).isNotEqualTo(b);
    }
}
