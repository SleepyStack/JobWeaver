package com.jobweaver.api.entity.simulation;

public record SleepStep(
        int durationMs
) implements SimulationStep {}
