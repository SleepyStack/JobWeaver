package com.jobweaver.common.messaging.simulation;

import java.util.List;

public record SimulationInstruction(
        List<SimulationStep> steps
) {}
