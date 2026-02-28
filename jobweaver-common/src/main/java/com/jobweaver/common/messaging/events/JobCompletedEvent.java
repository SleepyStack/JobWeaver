package com.jobweaver.common.messaging.events;

import java.util.UUID;

public record JobCompletedEvent(
        UUID jobId
)
{}
