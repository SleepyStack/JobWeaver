package com.jobweaver.api.controller;

import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.entity.JobType;
import com.jobweaver.api.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final JobService jobService;
    @PostMapping
    public String test(){
        Map<String, Object> payload = Map.of(
                "steps", List.of(
                        Map.of("action", "LOG", "message", "hello from /test"),
                        Map.of("action", "SLEEP", "durationMs", 250)
                )
        );
        UUID jobId = jobService.submitJob(new JobRequest(JobType.CPU_TASK, payload, 0));
        return "submitted " + jobId;
    }
}
