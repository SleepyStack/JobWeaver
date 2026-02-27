package com.jobweaver.api.entity.simulation;

public record LogStep(
        String message
) implements SimulationStep {}
