package com.jobweaver.api.controller;

import com.jobweaver.api.dto.CreateJobResponse;
import com.jobweaver.api.dto.JobPage;
import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.dto.JobResponse;
import com.jobweaver.api.service.JobService;
import com.jobweaver.common.messaging.enumeration.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<CreateJobResponse> create(@RequestBody JobRequest request) {
        log.info("Received job submission request type={}", request.jobType());
        CreateJobResponse response = jobService.submitJob(request);
        log.info("Job accepted jobId={} traceId={}", response.jobId(), response.traceId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable UUID id) {
        JobResponse response = jobService.getJob(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<JobPage> list(
            @RequestParam(required = false) JobType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JobPage result = jobService.listJobs(type, page, size);
        return ResponseEntity.ok(result);
    }
}
