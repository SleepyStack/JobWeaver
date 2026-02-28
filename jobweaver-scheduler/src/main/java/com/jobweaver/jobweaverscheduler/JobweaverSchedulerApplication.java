package com.jobweaver.jobweaverscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobweaverSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobweaverSchedulerApplication.class, args);
    }

}
