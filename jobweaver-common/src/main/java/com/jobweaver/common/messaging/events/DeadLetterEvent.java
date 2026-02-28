package com.jobweaver.common.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record DeadLetterEvent(
        UUID jobId,
        String reason,
        int finalRetryCount,
        Instant failedAt
) {}
