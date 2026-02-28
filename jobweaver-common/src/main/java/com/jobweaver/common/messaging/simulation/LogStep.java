package com.jobweaver.common.messaging.simulation;

public record LogStep(
        String message
) implements SimulationStep {}
