package com.jobweaver.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    private static final int CONSUMER_COUNT = 3;
    private static final int THREADS_PER_CONSUMER = 4;

    private static final int MAX_THREADS =
            CONSUMER_COUNT * THREADS_PER_CONSUMER;

    private static final int QUEUE_CAPACITY = 100;

    @Bean(name = "jobProcessorExecutor", destroyMethod = "shutdown")
    public ExecutorService jobProcessorExecutor() {

        return new ThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                Thread.ofPlatform()
                        .name("job-processor-", 0)
                        .factory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}