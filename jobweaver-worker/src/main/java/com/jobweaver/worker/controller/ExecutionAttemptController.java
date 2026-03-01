package com.jobweaver.worker.controller;

import com.jobweaver.worker.dto.ExecutionAttemptResponse;
import com.jobweaver.worker.service.ExecutionAttemptQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/executions")
@RequiredArgsConstructor
public class ExecutionAttemptController {

    private final ExecutionAttemptQueryService executionAttemptQueryService;

    @GetMapping("/{jobId}")
    public ResponseEntity<List<ExecutionAttemptResponse>> getExecutionAttempts(
            @PathVariable UUID jobId) {
        log.debug("Received execution attempts query for jobId={}", jobId);
        List<ExecutionAttemptResponse> attempts = executionAttemptQueryService.getAttemptsByJobId(jobId);
        return ResponseEntity.ok(attempts);
    }
}
