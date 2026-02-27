package com.jobweaver.api.dto;

import com.jobweaver.api.entity.simulation.SimulationInstruction;
import com.jobweaver.common.model.JobType;

import java.util.UUID;

public record JobSubmittedEvent(
      UUID jobId,
      JobType type,
      SimulationInstruction instruction
) {}
