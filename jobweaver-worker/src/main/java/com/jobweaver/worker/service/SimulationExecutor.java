package com.jobweaver.worker.service;

import com.jobweaver.common.messaging.simulation.*;
import com.jobweaver.worker.exception.SimulationFailureException;
import com.jobweaver.worker.exception.SimulationInterruptedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SimulationExecutor {

    public void execute(SimulationInstruction instruction, UUID jobId, String traceId) {

        for (SimulationStep step : instruction.steps()) {
            executeStep(step, jobId, traceId);
        }
    }

    private void executeStep(SimulationStep step, UUID jobId, String traceId) {

        switch (step) {

            case SleepStep s -> {
                log.info("SLEEP {}ms", s.durationMs());
                try {
                    Thread.sleep(s.durationMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SimulationInterruptedException(
                            "Sleep interrupted after " + s.durationMs() + "ms",
                            "SLEEP", jobId, traceId, e
                    );
                }
            }

            case LogStep l -> log.info("LOG: {}", l.message());

            case ComputeStep c -> {
                log.info("COMPUTE {} iterations", c.iterations());
                long acc = 0;
                for (int i = 0; i < c.iterations(); i++) {
                    acc += i;
                }
                log.debug("Compute result: {}", acc);
            }

            case HttpCallStep h -> {
                log.info("HTTP_CALL {} (simulated latency {}ms)",
                        h.url(), h.latencyMs());
                try {
                    Thread.sleep(h.latencyMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SimulationInterruptedException(
                            "HTTP call to " + h.url() + " interrupted",
                            "HTTP_CALL", jobId, traceId, e
                    );
                }
            }

            case FailStep f -> {
                throw new SimulationFailureException(
                        f.message(), "FAIL", jobId, traceId
                );
            }
        }
    }
}
