package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.events.RunJobEvent;
import com.jobweaver.common.messaging.simulation.*;
import com.jobweaver.worker.exception.SimulationFailureException;
import com.jobweaver.worker.exception.SimulationInterruptedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationExecutorTest {

    private final SimulationExecutor executor = new SimulationExecutor();
    private final UUID jobId = UUID.randomUUID();
    private final String traceId = UUID.randomUUID().toString();

    @Nested
    @DisplayName("execute - individual steps")
    class IndividualSteps {

        @Test
        @DisplayName("executes LogStep without error")
        void logStep() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new LogStep("hello world"))
            );

            // Should complete without exception
            executor.execute(instruction, jobId, traceId);
        }

        @Test
        @DisplayName("executes ComputeStep without error")
        void computeStep() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new ComputeStep(100))
            );

            executor.execute(instruction, jobId, traceId);
        }

        @Test
        @DisplayName("executes SleepStep with short duration")
        void sleepStep() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new SleepStep(10))
            );

            long start = System.currentTimeMillis();
            executor.execute(instruction, jobId, traceId);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(elapsed).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("executes HttpCallStep with simulated latency")
        void httpCallStep() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new HttpCallStep("https://example.com", 10))
            );

            long start = System.currentTimeMillis();
            executor.execute(instruction, jobId, traceId);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(elapsed).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("FailStep throws SimulationFailureException")
        void failStep() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new FailStep("simulated crash"))
            );

            assertThatThrownBy(() -> executor.execute(instruction, jobId, traceId))
                    .isInstanceOf(SimulationFailureException.class)
                    .hasMessage("simulated crash");
        }
    }

    @Nested
    @DisplayName("execute - multi-step sequences")
    class MultiStepSequences {

        @Test
        @DisplayName("executes all steps in order")
        void allStepsInOrder() {
            SimulationInstruction instruction = new SimulationInstruction(List.of(
                    new LogStep("step 1"),
                    new ComputeStep(10),
                    new LogStep("step 2")
            ));

            // Should complete all steps
            executor.execute(instruction, jobId, traceId);
        }

        @Test
        @DisplayName("stops execution at first FailStep")
        void stopsAtFailStep() {
            SimulationInstruction instruction = new SimulationInstruction(List.of(
                    new LogStep("before fail"),
                    new FailStep("boom"),
                    new LogStep("after fail - should not execute")
            ));

            assertThatThrownBy(() -> executor.execute(instruction, jobId, traceId))
                    .isInstanceOf(SimulationFailureException.class)
                    .hasMessage("boom");
        }

        @Test
        @DisplayName("handles empty instruction")
        void emptyInstruction() {
            SimulationInstruction instruction = new SimulationInstruction(List.of());
            executor.execute(instruction, jobId, traceId);
        }
    }

    @Nested
    @DisplayName("execute - interruption handling")
    class InterruptionHandling {

        @Test
        @DisplayName("SleepStep throws SimulationInterruptedException when thread is interrupted")
        void sleepInterrupted() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new SleepStep(60000))
            );

            Thread.currentThread().interrupt();

            assertThatThrownBy(() -> executor.execute(instruction, jobId, traceId))
                    .isInstanceOf(SimulationInterruptedException.class)
                    .hasMessageContaining("Sleep interrupted");

            // Clear interrupt flag
            Thread.interrupted();
        }

        @Test
        @DisplayName("HttpCallStep throws SimulationInterruptedException when thread is interrupted")
        void httpCallInterrupted() {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new HttpCallStep("https://example.com", 60000))
            );

            Thread.currentThread().interrupt();

            assertThatThrownBy(() -> executor.execute(instruction, jobId, traceId))
                    .isInstanceOf(SimulationInterruptedException.class)
                    .hasMessageContaining("interrupted");

            Thread.interrupted();
        }
    }
}
