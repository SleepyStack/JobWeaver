package com.jobweaver.jobweaverscheduler.controller;

import com.jobweaver.jobweaverscheduler.dto.JobExecutionResponse;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.service.JobExecutionQueryService;
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
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class JobExecutionController {

    private final JobExecutionQueryService jobExecutionQueryService;

    @GetMapping("/{id}/status")
    public ResponseEntity<JobExecutionResponse> getJobStatus(@PathVariable UUID id) {
        log.debug("Received status query for jobId={}", id);
        return ResponseEntity.ok(jobExecutionQueryService.getJobStatus(id));
    }

    @GetMapping
    public ResponseEntity<Page<JobExecutionResponse>> listJobs(
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<JobExecutionResponse> result = (status != null)
                ? jobExecutionQueryService.listByStatus(status, pageable)
                : jobExecutionQueryService.listAll(pageable);

        log.debug("Returning {} job executions (filter={}, page={}, size={})",
                result.getNumberOfElements(), status, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(result);
    }
}
