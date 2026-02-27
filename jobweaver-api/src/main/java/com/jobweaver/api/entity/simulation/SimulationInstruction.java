package com.jobweaver.api.entity.simulation;

import java.util.List;

public record SimulationInstruction(
        List<SimulationStep> steps
) {}
