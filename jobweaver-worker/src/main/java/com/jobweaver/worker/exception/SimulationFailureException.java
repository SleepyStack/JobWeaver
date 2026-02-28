package com.jobweaver.worker.exception;

import java.util.UUID;

/**
 * Thrown when a simulation step fails intentionally (e.g. a {@code FailStep})
 * or due to an unexpected error during step execution.
 */
public class SimulationFailureException extends WorkerException {

    private final String stepType;

    public SimulationFailureException(String message, String stepType, UUID jobId, String traceId) {
        super(message, ErrorCode.SIMULATION_FAILED, jobId, traceId);
        this.stepType = stepType;
    }

    public SimulationFailureException(String message, String stepType, UUID jobId, String traceId, Throwable cause) {
        super(message, ErrorCode.SIMULATION_FAILED, jobId, traceId, cause);
        this.stepType = stepType;
    }

    public String getStepType() { return stepType; }
}
