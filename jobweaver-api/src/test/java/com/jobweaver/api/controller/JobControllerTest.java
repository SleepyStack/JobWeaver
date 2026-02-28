package com.jobweaver.api.controller;

import com.jobweaver.api.dto.CreateJobResponse;
import com.jobweaver.api.dto.JobPage;
import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.dto.JobResponse;
import com.jobweaver.api.exceptions.GlobalExceptionHandler;
import com.jobweaver.api.exceptions.InvalidJobRequestException;
import com.jobweaver.api.exceptions.JobNotFoundException;
import com.jobweaver.api.service.JobService;
import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.common.messaging.simulation.SleepStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    private final SimulationInstruction validPayload = new SimulationInstruction(
            List.of(new SleepStep(100), new LogStep("test"))
    );

    @Nested
    @DisplayName("POST /api/jobs")
    class CreateJob {

        @Test
        @DisplayName("returns 202 ACCEPTED with valid request")
        void validRequest() throws Exception {
            UUID jobId = UUID.randomUUID();
            String traceId = UUID.randomUUID().toString();
            when(jobService.submitJob(any(JobRequest.class)))
                    .thenReturn(new CreateJobResponse(jobId, traceId));

            String requestBody = """
                    {
                        "jobType": "SIMULATION",
                        "payload": {
                            "steps": [
                                {"action": "SLEEP", "durationMs": 100},
                                {"action": "LOG", "message": "test"}
                            ]
                        },
                        "maxRetryCount": 3
                    }
                    """;

            mockMvc.perform(post("/api/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                    .andExpect(jsonPath("$.traceId").value(traceId));
        }

        @Test
        @DisplayName("returns 400 when request body is malformed JSON")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/api/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("API.INVALID_REQUEST"));
        }

        @Test
        @DisplayName("returns 400 when service throws InvalidJobRequestException")
        void invalidJobRequest() throws Exception {
            when(jobService.submitJob(any()))
                    .thenThrow(new InvalidJobRequestException("maxRetryCount must be non-negative", "maxRetryCount"));

            String requestBody = """
                    {
                        "jobType": "SIMULATION",
                        "payload": {"steps": [{"action": "LOG", "message": "x"}]},
                        "maxRetryCount": -1
                    }
                    """;

            mockMvc.perform(post("/api/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("API.INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("maxRetryCount must be non-negative"));
        }

        @Test
        @DisplayName("returns 500 when unexpected exception occurs")
        void unexpectedException() throws Exception {
            when(jobService.submitJob(any()))
                    .thenThrow(new RuntimeException("unexpected failure"));

            String requestBody = """
                    {
                        "jobType": "SIMULATION",
                        "payload": {"steps": [{"action": "LOG", "message": "x"}]},
                        "maxRetryCount": 0
                    }
                    """;

            mockMvc.perform(post("/api/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("API.INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("returns 400 for empty request body")
        void emptyBody() throws Exception {
            mockMvc.perform(post("/api/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───────────────── GET /api/jobs/{id} ─────────────────

    @Nested
    @DisplayName("GET /api/jobs/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 with job details when found")
        void returnsJob() throws Exception {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            JobResponse response = new JobResponse(id, JobType.SIMULATION,
                    "trace-1", now, now);

            when(jobService.getJob(id)).thenReturn(response);

            mockMvc.perform(get("/api/jobs/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.type").value("SIMULATION"))
                    .andExpect(jsonPath("$.traceId").value("trace-1"));
        }

        @Test
        @DisplayName("returns 404 when job not found")
        void returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(jobService.getJob(id)).thenThrow(new JobNotFoundException(id));

            mockMvc.perform(get("/api/jobs/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("API.JOB_NOT_FOUND"));
        }

        @Test
        @DisplayName("returns 400 for invalid UUID path variable")
        void invalidUuid() throws Exception {
            mockMvc.perform(get("/api/jobs/not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───────────────── GET /api/jobs ─────────────────

    @Nested
    @DisplayName("GET /api/jobs")
    class ListJobs {

        @Test
        @DisplayName("returns 200 with paginated job list")
        void returnsList() throws Exception {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            JobResponse job = new JobResponse(id, JobType.SIMULATION, "t", now, now);
            JobPage page = new JobPage(List.of(job), 0, 20, 1, 1);

            when(jobService.listJobs(eq(null), eq(0), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/jobs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobs").isArray())
                    .andExpect(jsonPath("$.jobs.length()").value(1))
                    .andExpect(jsonPath("$.jobs[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("passes type filter to service")
        void filtersbyType() throws Exception {
            JobPage page = new JobPage(List.of(), 0, 20, 0, 0);
            when(jobService.listJobs(eq(JobType.SIMULATION), eq(0), eq(20))).thenReturn(page);

            mockMvc.perform(get("/api/jobs").param("type", "SIMULATION"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobs").isEmpty());
        }

        @Test
        @DisplayName("passes custom page and size params")
        void customPagination() throws Exception {
            JobPage page = new JobPage(List.of(), 2, 5, 15, 3);
            when(jobService.listJobs(eq(null), eq(2), eq(5))).thenReturn(page);

            mockMvc.perform(get("/api/jobs")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }
    }
}
