package com.jobweaver.jobweaverscheduler.controller;

import com.jobweaver.jobweaverscheduler.dto.JobExecutionResponse;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.service.JobExecutionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<JobExecutionResponse>> listJobs(
            @RequestParam(required = false) JobStatus status) {

        List<JobExecutionResponse> result = (status != null)
                ? jobExecutionQueryService.listByStatus(status)
                : jobExecutionQueryService.listAll();

        log.debug("Returning {} job executions (filter={})", result.size(), status);
        return ResponseEntity.ok(result);
    }
}
