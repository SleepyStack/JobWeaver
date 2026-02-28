package com.jobweaver.common.messaging.simulation;

public record SleepStep(
        int durationMs
) implements SimulationStep {}
