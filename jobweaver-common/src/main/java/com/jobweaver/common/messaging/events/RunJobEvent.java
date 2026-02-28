package com.jobweaver.common.messaging.events;

import com.jobweaver.common.messaging.simulation.SimulationInstruction;

import java.util.UUID;

public record RunJobEvent(
        UUID jobId,
        SimulationInstruction instruction
) {}
