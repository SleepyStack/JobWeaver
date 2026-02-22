package com.jobweaver.worker.service;

import com.jobweaver.worker.kafka.JobCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WorkerService {
    @KafkaListener(
            topics = "job-execution-topic",
            groupId = "worker-group"
    )
    public void listen(Object obj){
        System.out.println("Received job: " +  obj);
    }
}
