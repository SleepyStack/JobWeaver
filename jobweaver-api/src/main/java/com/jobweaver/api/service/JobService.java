package com.jobweaver.api.service;

import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.repository.JobRepository;
import com.jobweaver.common.model.JobStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public void createJob(JobRequest req){
        Job job = new Job(
        req.jobType(),req.payload(), JobStatus.QUEUED,req.MaxRetryCount()
        );
        jobRepository.save(job);
    }

}
