package com.jobweaver.api.service;

import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.kafka.JobEventPublisher;
import com.jobweaver.api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventPublisher publisher;

    public UUID submitJob(JobRequest req) {
        String traceId = UUID.randomUUID().toString();
        Job  job = new Job(req.jobType(), req.payload(), traceId);
        jobRepository.save(job);

        String eventId =  UUID.randomUUID().toString();
        JobCreatedEvent event = new JobCreatedEvent(job.getId(),job.getType(),job.getInstruction(),req.maxRetryCount());
        publisher.publish(event, traceId, eventId);
        return job.getId();
    }
}
