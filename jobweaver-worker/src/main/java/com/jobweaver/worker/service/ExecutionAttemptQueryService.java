package com.jobweaver.worker.service;

import com.jobweaver.worker.dto.ExecutionAttemptResponse;
import com.jobweaver.worker.repository.ExecutionAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionAttemptQueryService {

    private final ExecutionAttemptRepository executionAttemptRepository;

    @Transactional(readOnly = true)
    public Page<ExecutionAttemptResponse> getAttemptsByJobId(UUID jobId, Pageable pageable) {
        return executionAttemptRepository.findByJobId(jobId, pageable)
                .map(ExecutionAttemptResponse::from);
    }
}
