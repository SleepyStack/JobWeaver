package com.jobweaver.api.dto;

import java.util.List;

/**
 * Paginated response wrapper for job listings.
 */
public record JobPage(
        List<JobResponse> jobs,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
