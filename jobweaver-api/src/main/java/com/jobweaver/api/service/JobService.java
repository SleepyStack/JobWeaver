package com.jobweaver.api.service;

import com.jobweaver.api.dto.CreateJobResponse;
import com.jobweaver.api.dto.JobPage;
import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.dto.JobResponse;
import com.jobweaver.api.exceptions.InvalidJobRequestException;
import com.jobweaver.api.exceptions.JobNotFoundException;
import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.kafka.JobEventPublisher;
import com.jobweaver.api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventPublisher publisher;

    @Transactional
    public CreateJobResponse submitJob(JobRequest req) {
        validateRequest(req);

        String traceId = UUID.randomUUID().toString();

        // Enrich MDC — the MdcFilter already sets a request-scoped traceId,
        // but here we overwrite with the job-specific traceId that will
        // follow this job across all services.
        MDC.put("traceId", traceId);

        try {
            Job job = new Job(req.jobType(), req.payload(), traceId);
            jobRepository.save(job);

            MDC.put("jobId", job.getId().toString());
            log.info("Job persisted type={}", req.jobType());

            String eventId = UUID.randomUUID().toString();
            JobCreatedEvent event = new JobCreatedEvent(
                    job.getId(), job.getType(), job.getInstruction(), req.maxRetryCount());
            publisher.publish(event, traceId, eventId);

            log.info("JobCreatedEvent published eventId={}", eventId);

            return new CreateJobResponse(job.getId(), traceId);
        } finally {
            MDC.remove("jobId");
            // traceId is managed by MdcFilter at the request scope
        }
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
        log.info("Retrieved job jobId={}", jobId);
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public JobPage listJobs(JobType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Job> result = (type != null)
                ? jobRepository.findByType(type, pageable)
                : jobRepository.findAll(pageable);

        log.info("Listed jobs page={} size={} total={} type={}", page, size, result.getTotalElements(), type);

        return new JobPage(
                result.getContent().stream().map(JobResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void validateRequest(JobRequest req) {
        if (req.jobType() == null) {
            throw new InvalidJobRequestException("jobType is required", "jobType");
        }
        if (req.payload() == null) {
            throw new InvalidJobRequestException("payload is required", "payload");
        }
        if (req.maxRetryCount() < 0) {
            throw new InvalidJobRequestException("maxRetryCount must be non-negative", "maxRetryCount");
        }
    }
}
