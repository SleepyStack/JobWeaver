package com.jobweaver.jobweaverscheduler.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DispatchSchedulerTest {

    @Mock
    private JobDispatchService dispatchService;

    @InjectMocks
    private DispatchScheduler dispatchScheduler;

    @Test
    @DisplayName("run() delegates to JobDispatchService.dispatchPendingJobs()")
    void delegatesToDispatchService() {
        dispatchScheduler.run();
        verify(dispatchService).dispatchPendingJobs();
    }
}
