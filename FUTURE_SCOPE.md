# JobWeaver -- Future Scope

> Current state assessment, known limitations, and the roadmap for Phase 3 development.

---

## Table of Contents

1. [Implemented Features (Phase 1-2)](#implemented-features-phase-1-2)
2. [Known Limitations](#known-limitations)
3. [Phase 3 Roadmap](#phase-3-roadmap)

---

## Implemented Features (Phase 1-2)

### Core Platform

- **REST API Gateway**: Full CRUD endpoints for job submission and retrieval with pagination, filtering, and structured error responses.
- **Event-Driven Architecture**: Five Kafka topics orchestrate the complete job lifecycle across three independently deployed services.
- **Database-per-Service Isolation**: Each service owns its PostgreSQL instance with Flyway-managed schema migrations.
- **Simulation Engine**: Pluggable step-based execution model using Java 21 sealed interfaces and pattern matching. Supports `SLEEP`, `LOG`, `COMPUTE`, `HTTP_CALL`, and `FAIL` step types.

### Reliability

- **Idempotent Event Processing**: Duplicate Kafka messages are detected and handled gracefully at both the scheduler (job ID as primary key) and worker (event ID as primary key) boundaries.
- **Retry with Exponential Backoff**: Failed jobs are automatically retried with configurable `maxRetries` and a capped exponential backoff strategy (up to 300 seconds).
- **Dead-Letter Queue**: Jobs that exhaust all retry attempts are routed to a dedicated `job-dead-letter` topic with full failure context.
- **Producer Idempotence**: All Kafka producers are configured with `enable.idempotence=true` and `acks=all`.
- **Optimistic Locking**: `@Version`-based concurrency control on scheduler entities prevents conflicting state transitions.

### Concurrency

- **Multi-Threaded Worker**: 3 Kafka consumer threads backed by a 12-thread processing pool with bounded queue and back-pressure via `CallerRunsPolicy`.
- **Asynchronous Offset Management**: Custom `PartitionState` and `OffsetCommitCoordinator` infrastructure enables out-of-order job completion while maintaining correct Kafka offset commits.
- **Skip-Locked Dispatch**: `SELECT ... FOR UPDATE SKIP LOCKED` enables concurrent scheduler instances without double-dispatching.

### Observability

- **Distributed Tracing**: UUID-based `traceId` propagated through Kafka headers and MDC across all service boundaries.
- **Structured Logging**: Logback with service name, trace ID, and job ID in every log line.
- **Health Checks**: Spring Boot Actuator endpoints integrated with Docker Compose health checks.

### Exception Handling

- **Layered Exception Hierarchy**: Module-scoped exception classes extending a shared `BaseDomainException`, with namespaced error codes (`API.*`, `SCHEDULER.*`, `WORKER.*`).
- **Global Exception Handler**: Centralised `@RestControllerAdvice` mapping domain, validation, and unexpected exceptions to standardised `ErrorResponse` payloads.

### Testing

- **143+ Unit Tests** across 32 test classes covering controllers, services, Kafka producers/consumers, entities, exception hierarchies, and async offset management.
- **Test Isolation**: All tests use Mockito-based mocking with no infrastructure dependencies.

### Infrastructure

- **Docker Compose**: Full local deployment with 7 containers (3 PostgreSQL, Zookeeper, Kafka, 3 application services).
- **Container Optimisation**: Alpine-based JRE images, non-root execution, container-aware JVM flags.

---

## Known Limitations

### Scheduling Fairness

The current dispatch strategy uses a `next_run_at` ordered query, which can lead to starvation of recently submitted jobs when the retry backlog is large. Jobs with earlier `next_run_at` values are consistently prioritised, regardless of how long other jobs have been waiting in their initial `PENDING` state.

### Synchronous Simulation Execution

The `SimulationExecutor` processes all steps sequentially on a single thread. Steps such as `HttpCallStep` simulate latency via `Thread.sleep` rather than performing actual non-blocking I/O. This means each step blocks its thread for the full duration, reducing the effective throughput of the worker thread pool under I/O-heavy workloads.

### Limited Worker Scalability Configuration

The thread pool size (12 threads) and consumer count (3) are hardcoded in configuration. There is no dynamic scaling based on workload, no auto-tuning of `max.poll.interval.ms` relative to expected job duration, and no mechanism to adjust concurrency at runtime.

### No Inter-Service Synchronous Communication

Services rely exclusively on Kafka for communication. There is no REST-based inter-service communication layer, which means:
- The API cannot query the scheduler for real-time job status.
- The dashboard has no mechanism to aggregate data from multiple services.
- There is no unified view of a job's lifecycle from submission to completion.

### Dashboard Not Implemented

The frontend module is a placeholder scaffold with no functional components. There is no user interface for monitoring, controlling, or inspecting the job execution pipeline.

### Integration Test Coverage

All tests are unit-level with mocked dependencies. There are no integration tests that verify end-to-end flows with real Kafka and PostgreSQL instances. The existing `@SpringBootTest` in the API module is disabled.

### Single-Partition Bottleneck

The `job-created` topic uses a single partition, which limits the throughput of job ingestion into the scheduler. Under high submission volumes, this becomes a serialisation bottleneck.

### No Metrics or Alerting

The platform lacks quantitative observability. There are no Prometheus metrics, Grafana dashboards, or alerting rules for monitoring job throughput, failure rates, retry counts, queue depths, or consumer lag.

### No Authentication or Authorisation

The API exposes unauthenticated endpoints. There is no API key validation, OAuth2 integration, or role-based access control.

### Dead-Letter Queue Has No Consumer

The `job-dead-letter` topic receives failed jobs but has no consumer processing or monitoring those events. Failed jobs are effectively lost once they reach the DLQ.

---

## Phase 3 Roadmap

### 3.1 Round-Robin Scheduling

**Priority**: High

Replace the current `next_run_at`-ordered dispatch with a round-robin or weighted fair queuing strategy. This ensures that newly submitted jobs are not starved by a backlog of retrying jobs.

Proposed approach:
- Introduce a `priority` or `queue_position` column in the `job_executions` table.
- Alternate dispatch between first-attempt jobs and retry jobs within each batch.
- Consider a two-queue model: one for fresh submissions, one for retries, with configurable dispatch ratios.

### 3.2 Frontend Dashboard

**Priority**: High

Develop the `jobweaver-dashboard` module into a fully functional monitoring and control interface. Target capabilities:

- **Job Lifecycle View**: Real-time tracking of job status from submission through completion or failure.
- **Execution History**: Detailed logs of each execution attempt, including step-by-step progress.
- **Retry Visualisation**: Timeline view of retry attempts with backoff intervals.
- **Dead-Letter Inspector**: Browse, inspect, and manually retry DLQ entries.
- **System Health**: Consumer lag, throughput metrics, service health status.
- **Job Submission**: Form-based job creation with instruction builder.
- **Filtering and Search**: By status, type, trace ID, date range.

Technology: React 19, Vite, with Axios for API communication. Likely requires WebSocket or SSE support for real-time updates.

### 3.3 Inter-Service REST Communication

**Priority**: High

Introduce synchronous REST endpoints between services to support aggregated queries and cross-service data access that Kafka alone cannot efficiently provide:

- **Scheduler exposes** `GET /internal/jobs/{id}/status` for real-time status queries.
- **Worker exposes** `GET /internal/executions/{jobId}` for execution attempt history.
- **API aggregates** data from scheduler and worker to provide a unified job detail view.
- Use Spring `WebClient` or `RestClient` for inter-service HTTP calls.
- Implement circuit breakers (Resilience4j) to handle service unavailability gracefully.
- Consider a lightweight API gateway or service discovery (e.g., Spring Cloud) for routing.

### 3.4 Asynchronous and Non-Blocking Execution

**Priority**: Medium

Refactor the worker's simulation execution to support genuinely asynchronous operations:

- Replace `Thread.sleep`-based HTTP simulation with actual non-blocking HTTP calls using `WebClient`.
- Introduce `CompletableFuture`-based step execution where applicable.
- Evaluate reactive execution for I/O-bound steps while retaining thread-per-job for CPU-bound steps.
- This would significantly improve thread pool utilisation under workloads dominated by `HttpCallStep` or `SleepStep`.

### 3.5 Integration and End-to-End Testing

**Priority**: Medium

Establish a comprehensive integration test suite:

- **Testcontainers**: Use Testcontainers for PostgreSQL and Kafka to run integration tests without external infrastructure.
- **End-to-end flow tests**: Submit a job via the API, verify it flows through the scheduler and worker, and confirm the final state in all three databases.
- **Retry scenario tests**: Verify that `FailStep` instructions trigger the correct number of retries with exponential backoff.
- **DLQ tests**: Confirm that exhausted retries produce dead-letter events.
- **Concurrent dispatch tests**: Validate `SKIP LOCKED` behaviour under multi-instance scheduling.
- **Consumer rebalance tests**: Verify offset management correctness during partition reassignment.

### 3.6 Observability and Metrics

**Priority**: Medium

Introduce quantitative monitoring:

- **Micrometer + Prometheus**: Instrument job submission rate, dispatch latency, execution duration, retry frequency, DLQ volume, and consumer lag.
- **Grafana Dashboards**: Pre-built dashboards for operational visibility.
- **Alerting**: Configurable alerts for abnormal failure rates, consumer lag thresholds, and DLQ accumulation.
- **Distributed Tracing**: Integrate OpenTelemetry or Micrometer Tracing for end-to-end trace visualisation (replacing the manual UUID-based approach).

### 3.7 Dead-Letter Queue Processing

**Priority**: Medium

Implement a DLQ consumer and management layer:

- DLQ consumer service (or integration within the scheduler) that persists dead-letter events.
- REST endpoints for DLQ inspection, manual retry, and permanent dismissal.
- Dashboard integration for DLQ management.
- Optional: configurable DLQ retention policies and automatic notification (e.g., webhook, email).

### 3.8 Security

**Priority**: Medium

- API key or JWT-based authentication on the REST API.
- Role-based access control (admin, operator, viewer).
- Internal service-to-service authentication (mTLS or shared secret).
- Input sanitisation and rate limiting.

### 3.9 Dynamic Scaling and Configuration

**Priority**: Low

- Externalise thread pool size, consumer count, batch size, and polling interval to application configuration (or a centralised config server).
- Explore Kubernetes-native scaling based on consumer lag metrics.
- Auto-scaling worker instances based on queue depth.

### 3.10 Multi-Partition Ingestion

**Priority**: Low

- Increase the `job-created` topic to multiple partitions to eliminate the single-partition serialisation bottleneck.
- Ensure the scheduler consumer group handles partition rebalancing correctly.

### 3.11 Additional Job Types

**Priority**: Low

- Extend `JobType` beyond `SIMULATION` to support arbitrary job execution (e.g., `BATCH_PROCESS`, `DATA_PIPELINE`, `WEBHOOK`).
- Implement a step registry or plugin architecture for custom step types.
- Consider a scripting engine (GraalVM, Groovy) for user-defined job logic.
