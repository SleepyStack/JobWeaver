package com.jobweaver.worker.service;

import com.jobweaver.worker.dto.ExecutionAttemptResponse;
import com.jobweaver.worker.repository.ExecutionAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionAttemptQueryService {

    private final ExecutionAttemptRepository executionAttemptRepository;

    @Transactional(readOnly = true)
    public List<ExecutionAttemptResponse> getAttemptsByJobId(UUID jobId) {
        return executionAttemptRepository.findByJobIdOrderByStartedAtDesc(jobId)
                .stream()
                .map(ExecutionAttemptResponse::from)
                .toList();
    }
}
