package com.jobweaver.api.dto;

import com.jobweaver.api.entity.simulation.SimulationInstruction;
import com.jobweaver.common.model.JobType;
import lombok.NonNull;


public record JobRequest(
        @NonNull
        JobType jobType,

        @NonNull
        SimulationInstruction payload,

        int maxRetryCount
                         )
{}
