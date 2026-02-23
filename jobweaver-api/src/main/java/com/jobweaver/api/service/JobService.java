package com.jobweaver.api.service;

import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.kafka.JobEventProducer;
import com.jobweaver.api.repository.JobRepository;
import com.jobweaver.common.model.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventProducer jobEventProducer;

    /**
     * Minimal vertical-slice behavior:
     * persist a QUEUED job and publish its id to Kafka for workers to pick up.
     */
    public UUID submitJob(JobRequest req) {
        Job job = new Job(req.jobType(), req.payload(), JobStatus.QUEUED, req.maxRetryCount());

        Job saved = jobRepository.save(job);
        jobEventProducer.sendMessage(saved.getId());
        return saved.getId();
    }

}
