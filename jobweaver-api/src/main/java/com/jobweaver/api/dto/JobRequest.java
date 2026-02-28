package com.jobweaver.api.dto;

import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import lombok.NonNull;


public record JobRequest(
        @NonNull
        JobType jobType,

        @NonNull
        SimulationInstruction payload,

        int maxRetryCount
                         )
{}
