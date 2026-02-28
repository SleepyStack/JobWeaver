package com.jobweaver.jobweaverscheduler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DispatchScheduler {

    private final JobDispatchService dispatchService;

    @Scheduled(fixedDelay = 3000)
    public void run() {
        dispatchService.dispatchPendingJobs();
    }
}
