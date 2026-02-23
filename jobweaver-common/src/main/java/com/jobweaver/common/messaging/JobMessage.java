package com.jobweaver.common.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobMessage {
    private UUID jobId;
    private Map<String,Object> payload;
    private int maxRetries;
}
