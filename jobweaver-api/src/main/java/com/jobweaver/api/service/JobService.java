package com.jobweaver.api.service;

import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.kafka.JobEventProducer;
import com.jobweaver.api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventProducer jobEventProducer;

    public UUID submitJob(JobRequest req) {
        String traceId = UUID.randomUUID().toString();

        Job job = new Job(req.jobType(), req.payload(), JobStatus.QUEUED, traceId , req.maxRetryCount());

        Job saved = jobRepository.save(job);

        String eventId = UUID.randomUUID().toString();

        jobEventProducer.sendMessage(saved.getId(),traceId, eventId );
        return saved.getId();
    }



    private UUID generateUUID() {
        return UUID.randomUUID();
    }

}
