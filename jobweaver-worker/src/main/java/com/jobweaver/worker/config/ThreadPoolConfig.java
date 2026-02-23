package com.jobweaver.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    /**
     * 3 Kafka consumer threads × 4 processing threads each = 12 total.
     * Each consumer dispatches work to this shared pool.
     */
    private static final int THREADS_PER_CONSUMER = 4;
    private static final int CONSUMER_COUNT = 3;

    @Bean(name = "jobProcessorExecutor", destroyMethod = "shutdown")
    public ExecutorService jobProcessorExecutor() {
        return Executors.newFixedThreadPool(
                CONSUMER_COUNT * THREADS_PER_CONSUMER,
                Thread.ofPlatform()
                        .name("job-processor-", 0)
                        .factory()
        );
    }
}
