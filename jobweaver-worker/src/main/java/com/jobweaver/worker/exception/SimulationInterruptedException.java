package com.jobweaver.worker.exception;

import java.util.UUID;

/**
 * Thrown when a simulation step is interrupted (e.g. thread interrupted
 * during {@code SleepStep} or {@code HttpCallStep}).
 */
public class SimulationInterruptedException extends WorkerException {

    private final String stepType;

    public SimulationInterruptedException(String message, String stepType, UUID jobId, String traceId, Throwable cause) {
        super(message, ErrorCode.SIMULATION_INTERRUPTED, jobId, traceId, cause);
        this.stepType = stepType;
    }

    public String getStepType() { return stepType; }
}
