package com.jobweaver.common.messaging.simulation;

public record HttpCallStep(
        String url,
        int latencyMs
) implements SimulationStep {}
