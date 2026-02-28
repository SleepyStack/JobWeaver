package com.jobweaver.api.controller;

import com.jobweaver.api.dto.CreateJobResponse;
import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<CreateJobResponse> create(@RequestBody JobRequest request) {
        CreateJobResponse response = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
