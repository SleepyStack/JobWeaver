package com.jobweaver.common.messaging.events;

import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;

import java.util.UUID;

public record JobCreatedEvent(
      UUID jobId,
      JobType type,
      SimulationInstruction instruction,
      int maxRetries
) {}
