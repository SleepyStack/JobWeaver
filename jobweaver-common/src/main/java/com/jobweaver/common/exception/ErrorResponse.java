package com.jobweaver.common.exception;

import java.time.LocalDateTime;
import java.util.UUID;

public record ErrorResponse(
        LocalDateTime timeStamp,
        int status,
        String errorCode,
        String message,
        UUID jobId
        )
{}
