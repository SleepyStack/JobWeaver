package com.jobweaver.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseDomainExceptionTest {

    @Test
    @DisplayName("stores message, errorCode, httpStatus, and jobId")
    void storesAllFields() {
        UUID jobId = UUID.randomUUID();

        BaseDomainException ex = new BaseDomainException(
                "Something went wrong", "API.TEST_ERROR", 500, jobId
        ) {};

        assertThat(ex.getMessage()).isEqualTo("Something went wrong");
        assertThat(ex.getErrorCode()).isEqualTo("API.TEST_ERROR");
        assertThat(ex.getHttpStatus()).isEqualTo(500);
        assertThat(ex.getJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("works with null jobId")
    void nullJobId() {
        BaseDomainException ex = new BaseDomainException(
                "No job", "API.GENERIC", 400, null
        ) {};

        assertThat(ex.getJobId()).isNull();
    }

    @Test
    @DisplayName("is a RuntimeException")
    void isRuntimeException() {
        BaseDomainException ex = new BaseDomainException(
                "test", "CODE", 500, null
        ) {};

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
