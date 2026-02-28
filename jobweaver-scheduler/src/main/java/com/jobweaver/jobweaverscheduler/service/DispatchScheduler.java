package com.jobweaver.jobweaverscheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchScheduler {

    private final JobDispatchService dispatchService;

    @Scheduled(fixedDelay = 10000)
    public void run() {
        log.debug("Dispatch tick started");
        dispatchService.dispatchPendingJobs();
    }
}
