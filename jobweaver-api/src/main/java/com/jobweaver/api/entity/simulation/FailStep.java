package com.jobweaver.api.entity.simulation;

public record FailStep(
        String message
) implements SimulationStep {}
