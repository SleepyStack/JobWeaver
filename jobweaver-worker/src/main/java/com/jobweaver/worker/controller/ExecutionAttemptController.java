package com.jobweaver.worker.controller;

import com.jobweaver.worker.dto.ExecutionAttemptResponse;
import com.jobweaver.worker.service.ExecutionAttemptQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/executions")
@RequiredArgsConstructor
public class ExecutionAttemptController {

    private final ExecutionAttemptQueryService executionAttemptQueryService;

    @GetMapping("/{jobId}")
    public ResponseEntity<Page<ExecutionAttemptResponse>> getExecutionAttempts(
            @PathVariable UUID jobId,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Received execution attempts query for jobId={}, page={}, size={}",
                jobId, pageable.getPageNumber(), pageable.getPageSize());
        Page<ExecutionAttemptResponse> attempts = executionAttemptQueryService.getAttemptsByJobId(jobId, pageable);
        return ResponseEntity.ok(attempts);
    }
}
