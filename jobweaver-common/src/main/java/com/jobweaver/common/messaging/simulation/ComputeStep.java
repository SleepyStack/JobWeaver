package com.jobweaver.common.messaging.simulation;

public record ComputeStep(
        int iterations
) implements SimulationStep {}
