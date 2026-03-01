package com.jobweaver.jobweaverscheduler.service;

import com.jobweaver.jobweaverscheduler.dto.JobExecutionResponse;
import com.jobweaver.jobweaverscheduler.entity.JobStatus;
import com.jobweaver.jobweaverscheduler.exception.JobNotFoundException;
import com.jobweaver.jobweaverscheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionQueryService {

    private final JobExecutionRepository jobExecutionRepository;

    @Transactional(readOnly = true)
    public JobExecutionResponse getJobStatus(UUID jobId) {
        return jobExecutionRepository.findById(jobId)
                .map(JobExecutionResponse::from)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> listByStatus(JobStatus status) {
        return jobExecutionRepository.findByJobStatus(status)
                .stream()
                .map(JobExecutionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> listAll() {
        return jobExecutionRepository.findAll()
                .stream()
                .map(JobExecutionResponse::from)
                .toList();
    }
}
