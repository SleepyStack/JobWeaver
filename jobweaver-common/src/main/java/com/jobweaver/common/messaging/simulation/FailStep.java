package com.jobweaver.common.messaging.simulation;

public record FailStep(
        String message
) implements SimulationStep {}
