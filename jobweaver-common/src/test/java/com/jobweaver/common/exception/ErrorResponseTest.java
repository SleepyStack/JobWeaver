package com.jobweaver.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    @DisplayName("ErrorResponse record stores all fields")
    void storesAllFields() {
        UUID jobId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ErrorResponse response = new ErrorResponse(now, 400, "API.BAD_REQUEST", "Invalid input", jobId);

        assertThat(response.timeStamp()).isEqualTo(now);
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo("API.BAD_REQUEST");
        assertThat(response.message()).isEqualTo("Invalid input");
        assertThat(response.jobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("ErrorResponse with null jobId")
    void nullJobId() {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(), 500, "API.INTERNAL", "Server error", null
        );

        assertThat(response.jobId()).isNull();
    }

    @Test
    @DisplayName("equals and hashCode correct for identical records")
    void equalsAndHashCode() {
        LocalDateTime time = LocalDateTime.of(2026, 2, 28, 12, 0);
        UUID jobId = UUID.randomUUID();

        ErrorResponse a = new ErrorResponse(time, 400, "CODE", "msg", jobId);
        ErrorResponse b = new ErrorResponse(time, 400, "CODE", "msg", jobId);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
