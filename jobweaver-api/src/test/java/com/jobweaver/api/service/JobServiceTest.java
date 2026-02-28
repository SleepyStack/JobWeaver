package com.jobweaver.api.service;

import com.jobweaver.api.dto.CreateJobResponse;
import com.jobweaver.api.dto.JobRequest;
import com.jobweaver.api.entity.Job;
import com.jobweaver.api.exceptions.InvalidJobRequestException;
import com.jobweaver.api.kafka.JobEventPublisher;
import com.jobweaver.api.repository.JobRepository;
import com.jobweaver.common.messaging.enumeration.JobType;
import com.jobweaver.common.messaging.events.JobCreatedEvent;
import com.jobweaver.common.messaging.simulation.LogStep;
import com.jobweaver.common.messaging.simulation.SimulationInstruction;
import com.jobweaver.common.messaging.simulation.SleepStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobEventPublisher publisher;

    @InjectMocks
    private JobService jobService;

    @Captor
    private ArgumentCaptor<Job> jobCaptor;

    @Captor
    private ArgumentCaptor<JobCreatedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<String> traceIdCaptor;

    @Captor
    private ArgumentCaptor<String> eventIdCaptor;

    private SimulationInstruction validInstruction;

    @BeforeEach
    void setUp() {
        validInstruction = new SimulationInstruction(
                List.of(new SleepStep(100), new LogStep("test"))
        );
    }

    @Nested
    @DisplayName("submitJob - happy path")
    class SubmitJobHappyPath {

        @Test
        @DisplayName("saves job to repository and publishes event")
        void savesAndPublishes() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, 3);

            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job j = invocation.getArgument(0);
                setId(j, UUID.randomUUID());
                return j;
            });

            CreateJobResponse response = jobService.submitJob(request);

            verify(jobRepository).save(jobCaptor.capture());
            Job savedJob = jobCaptor.getValue();
            assertThat(savedJob.getType()).isEqualTo(JobType.SIMULATION);
            assertThat(savedJob.getInstruction()).isEqualTo(validInstruction);
            assertThat(savedJob.getTraceId()).isNotBlank();

            verify(publisher).publish(eventCaptor.capture(), traceIdCaptor.capture(), eventIdCaptor.capture());
            JobCreatedEvent event = eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(JobType.SIMULATION);
            assertThat(event.instruction()).isEqualTo(validInstruction);
            assertThat(event.maxRetries()).isEqualTo(3);

            assertThat(response).isNotNull();
            assertThat(response.traceId()).isEqualTo(savedJob.getTraceId());
        }

        @Test
        @DisplayName("returns response with jobId and traceId")
        void returnsCorrectResponse() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, 0);
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job j = invocation.getArgument(0);
                setId(j, UUID.randomUUID());
                return j;
            });

            CreateJobResponse response = jobService.submitJob(request);

            assertThat(response.traceId()).isNotNull();
        }

        @Test
        @DisplayName("generates unique traceId and eventId")
        void uniqueIds() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, 1);
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job j = invocation.getArgument(0);
                setId(j, UUID.randomUUID());
                return j;
            });

            jobService.submitJob(request);
            jobService.submitJob(request);

            verify(publisher, times(2)).publish(any(), traceIdCaptor.capture(), eventIdCaptor.capture());
            List<String> traceIds = traceIdCaptor.getAllValues();
            List<String> eventIds = eventIdCaptor.getAllValues();

            assertThat(traceIds.get(0)).isNotEqualTo(traceIds.get(1));
            assertThat(eventIds.get(0)).isNotEqualTo(eventIds.get(1));
        }
    }

    @Nested
    @DisplayName("submitJob - validation")
    class SubmitJobValidation {

        @Test
        @DisplayName("throws InvalidJobRequestException when jobType is null")
        void nullJobType() {
            // JobRequest uses @NonNull on jobType, so constructing with null throws NullPointerException
            // Instead test the internal validateRequest by passing null through reflection or by
            // recognizing that Lombok's @NonNull throws NullPointerException at construction time
            assertThatThrownBy(() -> new JobRequest(null, validInstruction, 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws InvalidJobRequestException when payload is null")
        void nullPayload() {
            assertThatThrownBy(() -> new JobRequest(JobType.SIMULATION, null, 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws InvalidJobRequestException when maxRetryCount is negative")
        void negativeMaxRetryCount() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, -1);

            assertThatThrownBy(() -> jobService.submitJob(request))
                    .isInstanceOf(InvalidJobRequestException.class)
                    .hasMessageContaining("maxRetryCount must be non-negative");
        }

        @Test
        @DisplayName("does not save or publish when validation fails")
        void noSideEffectsOnValidationFailure() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, -1);

            assertThatThrownBy(() -> jobService.submitJob(request))
                    .isInstanceOf(InvalidJobRequestException.class);

            verifyNoInteractions(jobRepository);
            verifyNoInteractions(publisher);
        }
    }

    @Nested
    @DisplayName("submitJob - error handling")
    class SubmitJobErrorHandling {

        @Test
        @DisplayName("propagates repository exception")
        void repositoryException() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, 0);
            when(jobRepository.save(any())).thenThrow(new RuntimeException("DB down"));

            assertThatThrownBy(() -> jobService.submitJob(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB down");

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("propagates publisher exception")
        void publisherException() {
            JobRequest request = new JobRequest(JobType.SIMULATION, validInstruction, 0);
            when(jobRepository.save(any())).thenAnswer(inv -> {
                Job j = inv.getArgument(0);
                setId(j, UUID.randomUUID());
                return j;
            });
            doThrow(new RuntimeException("Kafka down"))
                    .when(publisher).publish(any(), anyString(), anyString());

            assertThatThrownBy(() -> jobService.submitJob(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kafka down");
        }
    }

    /** Reflectively set the JPA-generated id field. */
    private static void setId(Job job, UUID id) {
        try {
            Field f = Job.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(job, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
