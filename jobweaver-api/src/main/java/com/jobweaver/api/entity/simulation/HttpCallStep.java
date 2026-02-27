package com.jobweaver.api.entity.simulation;

public record HttpCallStep(
        String url,
        int latencyMs
) implements SimulationStep {}
