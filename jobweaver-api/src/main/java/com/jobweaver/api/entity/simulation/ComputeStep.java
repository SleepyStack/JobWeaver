package com.jobweaver.api.entity.simulation;

public record ComputeStep(
        int iterations
) implements SimulationStep {}
