# JobWeaver -- Architecture Analysis

> Distributed, event-driven job execution platform built on Spring Boot 4, Apache Kafka, and PostgreSQL.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Module Decomposition](#module-decomposition)
3. [Request Lifecycle](#request-lifecycle)
4. [Data Model](#data-model)
5. [Kafka Topology](#kafka-topology)
6. [Scheduling Engine](#scheduling-engine)
7. [Worker Execution Model](#worker-execution-model)
8. [Retry and Dead-Letter Strategy](#retry-and-dead-letter-strategy)
9. [Concurrency and Thread Model](#concurrency-and-thread-model)
10. [Offset Management](#offset-management)
11. [Idempotency Guarantees](#idempotency-guarantees)
12. [Exception Architecture](#exception-architecture)
13. [Observability](#observability)
14. [Infrastructure](#infrastructure)

---

## System Overview

JobWeaver is a multi-service system designed to accept, schedule, execute, and track simulation jobs. The platform follows an event-driven architecture where services communicate exclusively through Apache Kafka topics, with each service maintaining its own isolated PostgreSQL database (database-per-service pattern).

The system is composed of four backend modules and a frontend scaffold:

| Module | Role | Port |
|---|---|---|
| `jobweaver-api` | REST gateway; accepts and persists job requests | 8080 |
| `jobweaver-scheduler` | Consumes job events, schedules dispatch, manages retries | 8081 |
| `jobweaver-worker` | Executes simulation instructions, reports outcomes | 8082 |
| `jobweaver-common` | Shared domain objects, events, and exception base classes | N/A (library) |
| `jobweaver-dashboard` | React/Vite frontend (scaffold) | -- |

---

## Module Decomposition

### jobweaver-common

A pure library module (no Spring Boot application context) that defines the shared contracts consumed by all three services:

- **Event records**: `JobCreatedEvent`, `RunJobEvent`, `JobCompletedEvent`, `JobFailedEvent`, `DeadLetterEvent` -- all implemented as Java records for immutability.
- **Simulation model**: A sealed interface `SimulationStep` with Jackson polymorphic deserialization (`@JsonTypeInfo` / `@JsonSubTypes`). Subtypes: `SleepStep`, `LogStep`, `ComputeStep`, `HttpCallStep`, `FailStep`. The `SimulationInstruction` record wraps a `List<SimulationStep>`.
- **Exception base**: `BaseDomainException` (abstract `RuntimeException`) and `DomainErrorCode` interface, providing a consistent error structure across all services.
- **Enums**: `JobType` (`SIMULATION`), `ExecutionOutcome` (`RUNNING`, `SUCCESS`, `FAILURE`).
- **Error response**: `ErrorResponse` record standardises the API error body (`timestamp`, `status`, `errorCode`, `message`, `jobId`).

### jobweaver-api

The REST API gateway. Responsibilities:

1. Expose CRUD endpoints under `/api/jobs`.
2. Validate incoming `JobRequest` payloads.
3. Persist the `Job` entity (with JSONB-serialised `SimulationInstruction`) to the API database.
4. Publish a `JobCreatedEvent` to the `job-created` Kafka topic (synchronous send with `acks=all`).
5. Assign and propagate a `traceId` (UUID) via MDC and Kafka headers.

Key classes: `JobController`, `JobService`, `JobEventPublisher`, `MdcFilter`, `GlobalExceptionHandler`.

### jobweaver-scheduler

The scheduling and retry engine. Responsibilities:

1. Consume `JobCreatedEvent` messages and persist `JobExecution` records in `PENDING` state.
2. Poll the database on a fixed interval (10 seconds) for jobs whose `next_run_at` has elapsed.
3. Dispatch ready jobs by publishing `RunJobEvent` messages to the `run-job` topic.
4. Consume `JobCompletedEvent` and `JobFailedEvent` outcomes from the worker.
5. On failure: apply exponential backoff retry or route to the dead-letter topic.

Key classes: `IngestionService`, `SchedulerService`, `JobDispatchService`, `DispatchScheduler`, `RunJobPublisher`, `DeadLetterQueuePublisher`.

### jobweaver-worker

The job execution engine. Responsibilities:

1. Consume `RunJobEvent` messages from the `run-job` topic (3 consumer threads).
2. Submit simulation execution to a bounded thread pool (12 threads).
3. Execute the `SimulationInstruction` step-by-step via the `SimulationExecutor`.
4. Persist `ExecutionAttempt` records with idempotency guarantees (event ID as primary key).
5. Publish outcome events (`JobCompletedEvent` or `JobFailedEvent`) back to the scheduler.
6. Manage Kafka offsets asynchronously to allow out-of-order job completion.

Key classes: `RunJobListener`, `WorkerService`, `ExecutionAttemptProcessor`, `SimulationExecutor`, `PartitionState`, `OffsetCommitCoordinator`.

---

## Request Lifecycle

A job request flows through the system in the following stages:

### Stage 1 -- Submission (API)

1. Client sends `POST /api/jobs` with a `JobRequest` body containing `jobType`, `payload` (simulation instruction), and `maxRetryCount`.
2. `MdcFilter` (highest precedence) attaches or generates a `traceId`, placing it in MDC and echoing it as a response header.
3. `JobService.submitJob()` validates the request, generates a `traceId`, persists the `Job` entity to PostgreSQL, and publishes a `JobCreatedEvent` synchronously to the `job-created` topic.
4. The client receives a `202 Accepted` response with `jobId` and `traceId`.

### Stage 2 -- Ingestion (Scheduler)

5. `JobCreatedListener` receives the event, extracts the `traceId` header, and delegates to `IngestionService`.
6. `IngestionService.persistIfNotExists()` creates a `JobExecution` record in `PENDING` state with `retryCount=0` and `nextRunAt=Instant.now()`. Duplicate events (same `jobId`) are silently ignored via `DataIntegrityViolationException` catch.
7. The consumer acknowledges the message manually.

### Stage 3 -- Dispatch (Scheduler)

8. `DispatchScheduler` fires every 10 seconds, invoking `JobDispatchService.dispatchPendingJobs()`.
9. A `SELECT ... FOR UPDATE SKIP LOCKED` query retrieves up to 50 jobs where `status = PENDING` and `next_run_at <= now`.
10. Each job is marked `RUNNING` and a `RunJobEvent` is published asynchronously to the `run-job` topic (3 partitions, keyed by `jobId`).

### Stage 4 -- Execution (Worker)

11. `RunJobListener` receives the event on one of 3 consumer threads. It extracts `traceId` and `eventId` from Kafka headers.
12. The offset is registered as in-flight in the `PartitionState`, and the job is submitted to the `jobProcessorExecutor` thread pool.
13. `ExecutionAttemptProcessor.executeTransaction()` performs an idempotency check on `eventId`, creates the `ExecutionAttempt` record, and invokes `SimulationExecutor`.
14. `SimulationExecutor` iterates through the instruction steps: `SLEEP`, `LOG`, `COMPUTE`, `HTTP_CALL`, `FAIL`.
15. The attempt is marked `SUCCESS` or `FAILURE` in the database.

### Stage 5 -- Outcome Reporting (Worker to Scheduler)

16. `WorkerService` publishes either a `JobCompletedEvent` or `JobFailedEvent` synchronously to the corresponding Kafka topic.
17. The offset is marked completed in `PartitionState`. On the next poll cycle, contiguous completed offsets are committed.

### Stage 6 -- Completion or Retry (Scheduler)

18. `JobCompletedListener`: transitions the `JobExecution` from `RUNNING` to `COMPLETED`.
19. `JobFailedListener`: invokes `SchedulerService.handleFailure()`:
    - If `retryCount < maxRetries`: increments retry count, computes next backoff delay, resets status to `PENDING` with updated `nextRunAt`. The job re-enters dispatch on the next scheduler tick.
    - If `retryCount >= maxRetries`: marks the job `FAILED` and publishes a `DeadLetterEvent` to the `job-dead-letter` topic.

---

## Data Model

### API Database (`jobweaver_api`, port 5432)

```
jobs
├── id            UUID        PK
├── type          VARCHAR(50) NOT NULL          -- JobType enum
├── instruction   JSONB       NOT NULL          -- SimulationInstruction
├── trace_id      VARCHAR(100) NOT NULL
├── created_at    TIMESTAMPTZ NOT NULL          -- @CreatedDate
└── updated_at    TIMESTAMPTZ NOT NULL          -- @LastModifiedDate
Indexes: idx_jobs_trace_id, idx_jobs_created_at
```

### Scheduler Database (`jobweaver_scheduler`, port 5433)

```
job_executions
├── job_id        UUID        PK
├── trace_id      VARCHAR(100) NOT NULL
├── instruction   JSONB       NOT NULL
├── job_status    VARCHAR(30) NOT NULL          -- PENDING | RUNNING | COMPLETED | FAILED
├── retry_count   INTEGER     NOT NULL DEFAULT 0
├── max_retries   INTEGER     NOT NULL
├── next_run_at   TIMESTAMPTZ NOT NULL
├── last_error    TEXT
├── updated_at    TIMESTAMPTZ NOT NULL
└── version       BIGINT      NOT NULL DEFAULT 0  -- Optimistic locking
Indexes: idx_job_executions_status, idx_job_executions_next_run, idx_job_executions_trace_id
```

### Worker Database (`jobweaver_worker`, port 5434)

```
execution_attempts
├── event_id      UUID        PK               -- Kafka eventId (idempotency key)
├── job_id        UUID        NOT NULL
├── trace_id      VARCHAR(64) NOT NULL
├── started_at    TIMESTAMPTZ NOT NULL
├── finished_at   TIMESTAMPTZ
├── outcome       VARCHAR(20)                  -- RUNNING | SUCCESS | FAILURE
└── error_message TEXT
Indexes: idx_execution_attempts_job_id, idx_execution_attempts_started_at
```

All databases use Flyway for schema migration management.

---

## Kafka Topology

| Topic | Partitions | Replicas | Producer | Consumer | Event Type |
|---|---|---|---|---|---|
| `job-created` | 1 | 1 | API | Scheduler | `JobCreatedEvent` |
| `run-job` | 3 | 1 | Scheduler | Worker | `RunJobEvent` |
| `job-completed` | 3 | 1 | Worker | Scheduler | `JobCompletedEvent` |
| `job-failed` | 3 | 1 | Worker | Scheduler | `JobFailedEvent` |
| `job-dead-letter` | 1 | 1 | Scheduler | (none) | `DeadLetterEvent` |

**Producer configuration** (uniform across all modules):
- `acks=all` -- full ISR acknowledgment.
- `enable.idempotence=true` -- exactly-once semantics at the producer level.
- `retries=5` -- automatic retry on transient failures.
- Serialization: `StringSerializer` (key) + `JacksonJsonSerializer` (value), type-info headers disabled.

**Consumer configuration**:
- `enable.auto.commit=false` -- all modules use manual acknowledgment.
- Deserialization: `JacksonJsonDeserializer` with `setUseTypeHeaders(false)` and explicit target types.
- Worker-specific: `isolation.level=read_committed`, `max.poll.interval.ms=900000` (15 minutes).

**Message headers** (all topics): `traceId` (correlation), `eventId` (deduplication).

---

## Scheduling Engine

The scheduler module employs a database-backed polling strategy with pessimistic locking:

1. **Polling interval**: `@Scheduled(fixedDelay = 10000)` -- every 10 seconds after the previous dispatch completes.
2. **Query**: `SELECT * FROM job_executions WHERE job_status = 'PENDING' AND next_run_at <= :now ORDER BY next_run_at LIMIT 50 FOR UPDATE SKIP LOCKED`.
3. **SKIP LOCKED**: Enables horizontal scaling of scheduler instances without double-dispatching. Rows locked by one scheduler instance are simply skipped by others.
4. **Batch size**: Up to 50 jobs per dispatch cycle.
5. **Error isolation**: Failures during dispatch of individual jobs are caught and logged; remaining jobs in the batch continue processing.

The `JobExecution` entity uses `@Version` for optimistic locking, preventing concurrent state modifications from race conditions between the dispatch thread and Kafka listener threads.

---

## Worker Execution Model

The worker implements the multi-threaded Kafka consumer pattern as described in the Confluent architecture guide:

1. **Consumer threads**: 3 (configured via `spring.kafka.listener.concurrency`), each running an independent Kafka consumer within the same consumer group (`jobweaver-workers`).
2. **Processing thread pool**: 12 threads (`ArrayBlockingQueue(100)`, `CallerRunsPolicy`).
3. **Simulation execution**: The `SimulationExecutor` processes `SimulationInstruction` steps sequentially using Java 21 pattern matching (`switch` expressions over sealed types):
   - `SleepStep`: `Thread.sleep(durationMs)`
   - `LogStep`: Logs the message at INFO level
   - `ComputeStep`: CPU-bound loop accumulating a sum for `iterations` cycles
   - `HttpCallStep`: Simulated latency via `Thread.sleep(latencyMs)`
   - `FailStep`: Throws `SimulationFailureException` immediately, halting execution
4. **Transaction boundary**: `ExecutionAttemptProcessor` is a separate Spring bean from `WorkerService` to ensure `@Transactional` proxying functions correctly (avoids self-invocation).

---

## Retry and Dead-Letter Strategy

The retry mechanism is managed entirely by the scheduler module:

1. On receiving a `JobFailedEvent`, the scheduler increments `retryCount` and evaluates against `maxRetries`.
2. If retries remain, an exponential backoff delay is computed:

   ```
   delay = min(5 * 2^retryCount, 300) seconds
   ```

   | Retry | Delay |
   |---|---|
   | 1 | 10s |
   | 2 | 20s |
   | 3 | 40s |
   | 4 | 80s |
   | 5 | 160s |
   | 6+ | 300s (cap) |

3. The `JobExecution` status is reset to `PENDING` with `nextRunAt` set to `Instant.now() + delay`. The next dispatch cycle picks it up once the delay has elapsed.
4. If `retryCount > maxRetries`, the job is marked `FAILED` and a `DeadLetterEvent` is published to the `job-dead-letter` topic containing `jobId`, `reason`, `finalRetryCount`, and `failedAt`.

---

## Concurrency and Thread Model

| Component | Mechanism | Purpose |
|---|---|---|
| Scheduler dispatch query | `FOR UPDATE SKIP LOCKED` | Prevents double-dispatch across scheduler instances |
| `JobExecution.version` | Optimistic locking (`@Version`) | Prevents concurrent state transition conflicts |
| Worker consumer threads | 3 Kafka consumers (same group) | Parallelises message consumption across partitions |
| Worker processing pool | 12 threads, bounded queue (100) | Decouples I/O-bound simulation from Kafka poll loop |
| Back-pressure | `CallerRunsPolicy` | When pool is saturated, listener thread executes the task, naturally slowing `poll()` |
| `PartitionState` | `synchronized` methods | Thread-safe offset tracking across consumer and pool threads |
| `PartitionStateRegistry` | `ConcurrentHashMap` | Thread-safe partition-to-state mapping |

---

## Offset Management

The worker module implements a custom asynchronous offset commit strategy to handle out-of-order job completion:

1. **In-flight tracking**: When a record is received, its offset is registered in `PartitionState.inFlight` (a `TreeSet<Long>`).
2. **Completion marking**: When a job finishes (success or failure), its offset moves from `inFlight` to `completed`.
3. **Contiguous watermark**: `tryAdvanceCommit()` computes the highest contiguous completed offset from `lastCommittedOffset`. For example, if `lastCommittedOffset = 4` and `completed = {5, 6, 8}`, the watermark advances to 6 (gap at 7 blocks further advancement).
4. **Commit piggybacking**: Offset commits occur on the consumer thread during the next `poll()` cycle, via `OffsetCommitCoordinator.commitIfReady()`.
5. **Rebalance handling**: `ConsumerRebalanceHandler` flushes pending commits for revoked partitions and resets partition state.
6. **Stuck offset detection**: Warnings are logged if the completed set exceeds 10,000 entries, indicating a potentially stuck offset.

---

## Idempotency Guarantees

| Boundary | Mechanism |
|---|---|
| API to Scheduler | `jobId` (UUID) as primary key in `job_executions`. Duplicate `JobCreatedEvent` messages produce a `DataIntegrityViolationException`, which is caught silently. |
| Scheduler to Worker | `eventId` (UUID, generated per Kafka message) as primary key in `execution_attempts`. Duplicate `RunJobEvent` messages are detected, and the existing outcome is re-published without re-execution. |
| Producer idempotence | All Kafka producers use `enable.idempotence=true`, ensuring exactly-once delivery at the broker level. |

---

## Exception Architecture

Each module defines a local exception hierarchy extending `BaseDomainException` from `jobweaver-common`:

```
BaseDomainException (common)
├── ApiException (api)
│   ├── JobNotFoundException             (404)
│   ├── InvalidJobRequestException       (400)
│   ├── DuplicateJobException            (409)
│   ├── InvalidStateTransitionException  (409)
│   └── EventPublishException            (500)
├── SchedulerException (scheduler)
│   ├── JobNotFoundException             (404)
│   ├── InvalidStateTransitionException  (409)
│   ├── DispatchException                (500)
│   └── EventPublishException            (500)
└── WorkerException (worker)
    ├── SimulationFailureException       (500)
    ├── SimulationInterruptedException   (500)
    ├── EventPublishException            (500)
    ├── MalformedRecordException         (400)
    └── OffsetCommitException            (500)
```

Each module defines a `DomainErrorCode` enum implementing the `DomainErrorCode` interface, producing namespaced error codes (e.g., `API.JOB_NOT_FOUND`, `SCHEDULER.DISPATCH_FAILED`, `WORKER.SIMULATION_FAILED`).

The API module includes a `GlobalExceptionHandler` (`@RestControllerAdvice`) that maps domain exceptions to standardised `ErrorResponse` payloads with appropriate HTTP status codes.

---

## Observability

- **Structured logging**: All services use Logback with a consistent pattern: `[service=<name>] [traceId=%X{traceId}] [jobId=%X{jobId}]`.
- **Trace propagation**: A UUID-based `traceId` is generated at the API layer, propagated through Kafka headers, and restored into MDC at each service boundary.
- **MDC lifecycle**: `MdcFilter` (API) and all Kafka listeners set and clear MDC fields in `try/finally` blocks.
- **Health checks**: All Docker services expose Spring Boot Actuator `/actuator/health` endpoints, used as Docker Compose health checks.

---

## Infrastructure

### Docker Compose Services (7 containers)

| Service | Image | Exposed Port |
|---|---|---|
| `postgres-api` | `postgres:16` | 5432 |
| `postgres-scheduler` | `postgres:16` | 5433 |
| `postgres-worker` | `postgres:16` | 5434 |
| `zookeeper` | `confluentinc/cp-zookeeper:7.6.1` | 2181 |
| `kafka` | `confluentinc/cp-kafka:7.6.1` | 9092 |
| `jobweaver-api` | Custom (Dockerfile) | 8080 |
| `jobweaver-scheduler` | Custom (Dockerfile) | 8081 |
| `jobweaver-worker` | Custom (Dockerfile) | 8082 |

### Application Container Configuration

- Base image: `eclipse-temurin:21-jre-alpine`
- Non-root execution (`appuser:appgroup`)
- JVM flags: `-Duser.timezone=Asia/Kolkata`, `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`
- Spring profile: `docker` (overrides database hosts and Kafka bootstrap servers)

### Database Isolation

Each service maintains its own PostgreSQL instance with an independent schema, enforcing strict data ownership boundaries. Cross-service data access is performed exclusively through Kafka events.
