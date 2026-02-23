package com.jobweaver.api.controller;

import com.jobweaver.api.dto.CreateJobResponse;
import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<CreateJobResponse> create(@RequestBody JobRequest request) {
        UUID jobId = jobService.submitJob(request);
        return ResponseEntity.ok(new CreateJobResponse(jobId));
    }
}
