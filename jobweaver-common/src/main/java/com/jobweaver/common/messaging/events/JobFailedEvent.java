package com.jobweaver.common.messaging.events;

import java.util.UUID;

public record JobFailedEvent(
        UUID jobId,
        String errorMessage
)
{}
