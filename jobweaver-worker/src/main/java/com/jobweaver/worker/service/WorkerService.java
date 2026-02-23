package com.jobweaver.worker.service;
import com.jobweaver.worker.entity.Job;
import com.jobweaver.worker.exception.ErrorCode;
import com.jobweaver.worker.exception.WorkerException;
import com.jobweaver.worker.kafka.JobCreatedEvent;
import com.jobweaver.worker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final JobRepository jobRepository;

    @KafkaListener(
            topics = "job-execution-topic",
            groupId = "worker-group"
    )
    public void listen(JobCreatedEvent event){
        Job job = jobRepository.findById(event.getJobId())
                .orElseThrow(() -> new WorkerException("Job Not Found", ErrorCode.JOB_NOT_FOUND,event.getJobId()));
    }
}
