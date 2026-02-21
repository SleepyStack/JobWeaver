package com.jobweaver.api.dto;

import com.jobweaver.common.model.JobType;
import lombok.NonNull;

import java.util.Map;

public record JobRequest(
        @NonNull
        JobType jobType,

        @NonNull
        Map<String, Object> payload,

        @NonNull
        int MaxRetryCount
                         )
{}
