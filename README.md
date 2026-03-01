# JobWeaver

A distributed, event-driven job execution platform built on Spring Boot 4, Apache Kafka, and PostgreSQL. JobWeaver accepts simulation job requests via a REST API, schedules them with configurable retry logic, dispatches them to a pool of worker threads via Kafka, and tracks execution outcomes across isolated databases.

---

## Table of Contents

- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Modules](#modules)
- [Key Design Decisions](#key-design-decisions)
- [API Reference](#api-reference)
- [Local Setup](#local-setup)
- [Running Tests](#running-tests)
- [Documentation](#documentation)
- [References](#references)

---

## Architecture

JobWeaver follows a microservices architecture with event-driven communication through Apache Kafka. Each service owns its database and communicates exclusively via Kafka topics, enforcing strict data ownership boundaries.

For the complete architecture breakdown, see:
- [Architecture Analysis](ARCHITECTURE.md) -- detailed textual description of every component, data model, and design pattern.
- [Architecture Diagrams](ARCHITECTURE_DIAGRAM.md) -- Mermaid diagrams covering the system topology, request flow, state machine, thread model, and infrastructure layout.

### High-Level Flow

```
Client --> API (validate, persist, publish) --> Kafka [job-created]
    --> Scheduler (ingest, poll, dispatch) --> Kafka [run-job]
    --> Worker (execute, report) --> Kafka [job-completed / job-failed]
    --> Scheduler (complete or retry with backoff / dead-letter)
```

---

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.2 |
| Messaging | Apache Kafka (Confluent Platform) | 7.6.1 |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | Managed by Spring Boot |
| Serialisation | Jackson (JSON / JSONB) | 2.18.3 |
| Build | Maven (multi-module) | 3.x |
| Containers | Docker, Docker Compose | v3.9 |
| JVM Runtime | Eclipse Temurin (Alpine) | 21 |
| Frontend | React, Vite | 19.2, 7.2 |
| Testing | JUnit 5, Mockito, AssertJ | -- |
| Code Generation | Lombok | -- |

---

## Project Structure

```
jobweaver/
├── docker-compose.yml              # Full infrastructure + application stack
├── pom.xml                         # Parent POM (module declarations, dependency management)
├── ARCHITECTURE.md                 # Architecture analysis document
├── ARCHITECTURE_DIAGRAM.md         # Mermaid architecture diagrams
├── FUTURE_SCOPE.md                 # Roadmap, limitations, and Phase 3 plans
├── jobweaver-common/               # Shared library (events, exceptions, simulation model)
│   ├── pom.xml
│   └── src/main/java/com/jobweaver/common/
│       ├── events/                 #   Kafka event records
│       ├── exceptions/             #   Base exception classes
│       └── messaging/              #   SimulationInstruction, step types, enums
├── jobweaver-api/                  # REST API gateway (port 8080)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/jobweaver/api/
│       │   ├── config/             #   JPA auditing config
│       │   ├── controller/         #   JobController (REST endpoints)
│       │   ├── dto/                #   Request/response records
│       │   ├── entity/             #   Job entity (JPA + JSONB)
│       │   ├── exceptions/         #   API exception hierarchy + global handler
│       │   ├── filter/             #   MDC filter (traceId)
│       │   ├── kafka/              #   Producer config, admin, event publisher
│       │   ├── repository/         #   JPA repository
│       │   └── service/            #   JobService
│       └── main/resources/
│           ├── application.yaml
│           ├── logback-spring.xml
│           └── db/migration/       #   Flyway SQL migrations
├── jobweaver-scheduler/            # Scheduling + retry engine (port 8081)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/jobweaver/jobweaverscheduler/
│       ├── entity/                 #   JobExecution entity + JobStatus enum
│       ├── exceptions/             #   Scheduler exception hierarchy
│       ├── kafka/                  #   Consumers, producers, admin config
│       ├── repository/             #   JobExecutionRepository (SKIP LOCKED)
│       └── service/                #   IngestionService, SchedulerService, DispatchScheduler
├── jobweaver-worker/               # Job execution engine (port 8082)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/jobweaver/worker/
│       ├── config/                 #   Thread pool configuration
│       ├── entity/                 #   ExecutionAttempt entity
│       ├── exceptions/             #   Worker exception hierarchy
│       ├── kafka/                  #   Consumer, producer, async offset management
│       ├── repository/             #   ExecutionAttemptRepository
│       └── service/                #   WorkerService, SimulationExecutor, AttemptProcessor
└── jobweaver-dashboard/            # React frontend (scaffold)
    └── my-app/
        ├── package.json
        ├── vite.config.js
        └── src/
```

---

## Modules

### jobweaver-common

Shared library containing the domain contracts consumed by all three services:

- **Event records**: `JobCreatedEvent`, `RunJobEvent`, `JobCompletedEvent`, `JobFailedEvent`, `DeadLetterEvent`.
- **Simulation model**: Sealed `SimulationStep` interface with Jackson polymorphic deserialization. Step types: `SleepStep`, `LogStep`, `ComputeStep`, `HttpCallStep`, `FailStep`.
- **Exception base**: `BaseDomainException` and `DomainErrorCode` interface for namespaced error codes.

### jobweaver-api

REST gateway responsible for accepting job requests, persisting them, and publishing creation events. Exposes three endpoints under `/api/jobs`. Includes an MDC filter for trace ID propagation and a global exception handler for standardised error responses.

### jobweaver-scheduler

Consumes job creation events, persists execution records, and dispatches ready jobs on a 10-second polling interval using `SELECT ... FOR UPDATE SKIP LOCKED`. Manages retry logic with exponential backoff (capped at 300 seconds) and routes exhausted jobs to a dead-letter topic.

### jobweaver-worker

Consumes dispatch events across 3 Kafka consumer threads, submits execution to a 12-thread pool, and runs simulation instructions step-by-step. Implements custom asynchronous offset management to handle out-of-order job completion. Uses `CallerRunsPolicy` for natural back-pressure when the thread pool is saturated.

### jobweaver-dashboard

React/Vite scaffold. Planned for Phase 3 development as a full monitoring and control interface.

---

## Key Design Decisions

| Concern | Approach |
|---|---|
| **Event-driven decoupling** | Services communicate exclusively via Kafka; no synchronous inter-service calls. |
| **Database-per-service** | Three isolated PostgreSQL instances enforce strict data ownership. |
| **Idempotent processing** | Duplicate events are detected via primary key constraints (job ID at scheduler, event ID at worker). |
| **Optimistic locking** | `@Version` on scheduler entities prevents concurrent state transition conflicts. |
| **Pessimistic dispatch** | `FOR UPDATE SKIP LOCKED` enables concurrent scheduler instances without double-dispatching. |
| **Async offset commits** | Custom `PartitionState` / `OffsetCommitCoordinator` allows out-of-order completion with correct watermark-based commits. |
| **Back-pressure** | `CallerRunsPolicy` on the worker thread pool slows Kafka polling when processing capacity is exceeded. |
| **Exponential backoff** | `min(5 * 2^retryCount, 300)` seconds between retry attempts. |
| **Structured logging** | Logback with `[service=...] [traceId=...] [jobId=...]` across all services. |
| **Sealed types** | Java 21 sealed interfaces + pattern matching for type-safe simulation step dispatch. |

---

## API Reference

### Submit a Job

```
POST /api/jobs
Content-Type: application/json
```

**Request body:**

```json
{
  "jobType": "SIMULATION",
  "payload": {
    "steps": [
      { "action": "LOG", "message": "Starting job" },
      { "action": "SLEEP", "durationMs": 2000 },
      { "action": "COMPUTE", "iterations": 500000 },
      { "action": "HTTP_CALL", "url": "https://example.com", "latencyMs": 1500 }
    ]
  },
  "maxRetryCount": 3
}
```

**Response (`202 Accepted`):**

```json
{
  "jobId": "a1b2c3d4-...",
  "traceId": "e5f6g7h8-..."
}
```

### Get Job by ID

```
GET /api/jobs/{id}
```

**Response (`200 OK`):**

```json
{
  "id": "a1b2c3d4-...",
  "type": "SIMULATION",
  "traceId": "e5f6g7h8-...",
  "createdAt": "2026-02-28T10:00:00Z",
  "updatedAt": "2026-02-28T10:00:00Z"
}
```

### List Jobs (Paginated)

```
GET /api/jobs?type=SIMULATION&page=0&size=20
```

**Response (`200 OK`):**

```json
{
  "jobs": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

### Simulation Step Types

| Step | Fields | Behaviour |
|---|---|---|
| `SLEEP` | `durationMs` (int) | Pauses execution for the specified duration |
| `LOG` | `message` (String) | Logs the message at INFO level |
| `COMPUTE` | `iterations` (int) | CPU-bound loop (sum accumulation) |
| `HTTP_CALL` | `url` (String), `latencyMs` (int) | Simulates HTTP latency via sleep |
| `FAIL` | `message` (String) | Throws a failure exception, halting execution |

### Internal Endpoints (Lifecycle Tracking)

These endpoints are exposed directly by the scheduler and worker for observing the full job lifecycle. They are not part of the public API gateway.

#### Scheduler (`:8081`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/internal/jobs/{id}/status` | Execution status for a single job |
| `GET` | `/internal/jobs` | List all job executions |
| `GET` | `/internal/jobs?status=PENDING` | Filter executions by status (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`) |

**Example response (`GET /internal/jobs/{id}/status`):**

```json
{
  "jobId": "a1b2c3d4-...",
  "traceId": "e5f6g7h8-...",
  "jobStatus": "COMPLETED",
  "retryCount": 0,
  "maxRetries": 3,
  "nextRunAt": null,
  "updatedAt": "2026-03-01T06:17:00Z",
  "lastError": null
}
```

#### Worker (`:8082`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/internal/executions/{jobId}` | Execution attempt history for a job (newest first) |

**Example response:**

```json
[
  {
    "eventId": "f1e2d3c4-...",
    "jobId": "a1b2c3d4-...",
    "traceId": "e5f6g7h8-...",
    "outcome": "SUCCESS",
    "startedAt": "2026-03-01T06:16:55Z",
    "finishedAt": "2026-03-01T06:17:00Z",
    "errorMessage": null
  }
]
```

---

## Local Setup

### Prerequisites

- **Java 21** (JDK)
- **Maven 3.x**
- **Docker** and **Docker Compose**
- **Postman** (optional, for the included test collection)

### Step 1 -- Build the Project

Build all modules from the project root. **This must be done before starting Docker**, as the Dockerfiles copy the built JARs from each module's `target/` directory.

```bash
mvn clean package -DskipTests
```

This compiles `jobweaver-common`, `jobweaver-api`, `jobweaver-scheduler`, and `jobweaver-worker`, producing fat JARs in each module's `target/` directory.

### Step 2 -- Start the Infrastructure

Launch all services using Docker Compose:

```bash
docker compose up -d --build
```

This starts:

| Service | Port | Purpose |
|---|---|---|
| `postgres-api` | 5432 | API database (`jobweaver_api`) |
| `postgres-scheduler` | 5433 | Scheduler database (`jobweaver_scheduler`) |
| `postgres-worker` | 5434 | Worker database (`jobweaver_worker`) |
| `zookeeper` | 2181 | Kafka coordination |
| `kafka` | 9092 | Message broker |
| `jobweaver-api` | 8080 | REST API |
| `jobweaver-scheduler` | 8081 | Scheduler service (+ internal status endpoints) |
| `jobweaver-worker` | 8082 | Worker service (+ internal execution history endpoint) |

Docker Compose activates the `docker` Spring profile, which overrides database hosts and Kafka bootstrap servers to use Docker network hostnames.

### Step 3 -- Verify Services

Check that all services are healthy:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### Step 4 -- Submit and Track a Job

You can use the included **Postman collection** ([`JobWeaver.postman_collection.json`](JobWeaver.postman_collection.json)) which has pre-built requests for job submission, lifecycle tracking, and health checks. Import it into Postman and the collection variables (`api_url`, `scheduler_url`, `worker_url`) are pre-configured.

Or submit a test job manually:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "SIMULATION",
    "payload": {
      "steps": [
        { "action": "LOG", "message": "Hello from JobWeaver" },
        { "action": "SLEEP", "durationMs": 3000 },
        { "action": "COMPUTE", "iterations": 100000 }
      ]
    },
    "maxRetryCount": 2
  }'
```

Then track the lifecycle using the internal endpoints:

```bash
# Check execution status on the scheduler
curl http://localhost:8081/internal/jobs/{jobId}/status

# View execution attempts on the worker
curl http://localhost:8082/internal/executions/{jobId}
```

### Step 5 -- Monitor Execution

Observe logs across all services:

```bash
docker compose logs -f jobweaver-api jobweaver-scheduler jobweaver-worker
```

The structured logging output includes `traceId` and `jobId` for tracing the job across services.

### Stopping the Stack

```bash
docker-compose down -v
```

The `-v` flag removes named volumes, clearing all database state.

---

## Running Tests

Run the full test suite across all modules:

```bash
mvn test
```

Run tests for a specific module:

```bash
mvn test -pl jobweaver-api
mvn test -pl jobweaver-scheduler
mvn test -pl jobweaver-worker
mvn test -pl jobweaver-common
```

All tests are unit-level and require no external infrastructure. The test suite includes 143+ tests across 32 test classes.

---

## Documentation

| Document | Description |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Complete architecture analysis covering module decomposition, data model, Kafka topology, scheduling engine, worker execution model, retry strategy, concurrency, offset management, idempotency, exception architecture, and observability. |
| [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md) | Mermaid diagrams: high-level system architecture, end-to-end sequence flow, Kafka topic flow, job state machine, worker thread model, retry/backoff flow, database layout, and Docker Compose infrastructure. |
| [FUTURE_SCOPE.md](FUTURE_SCOPE.md) | Current feature inventory, known limitations, and the Phase 3 development roadmap including round-robin scheduling, frontend dashboard, inter-service REST communication, async execution, integration testing, observability, and security. |

---

## References

1. **Kafka Consumer Multi-Threaded Messaging** -- Confluent. The primary reference for the worker module's multi-threaded consumer architecture, covering consumer-per-thread vs. decoupled processing patterns, and the offset management challenges that arise from asynchronous job execution.
   https://www.confluent.io/blog/kafka-consumer-multi-threaded-messaging

2. **Erta, B. (2019). Optimistic Locking in JPA** -- Baeldung. Reference for the `@Version`-based optimistic locking strategy used on the `JobExecution` entity to prevent concurrent state transition conflicts between the dispatch thread and Kafka listener threads.
   https://www.baeldung.com/jpa-optimistic-locking

3. **PostgreSQL SELECT FOR UPDATE SKIP LOCKED** -- PostgreSQL Documentation. Foundation for the scheduler's concurrent dispatch strategy, enabling multiple scheduler instances to poll for ready jobs without double-dispatching through row-level advisory locking.
   https://www.postgresql.org/docs/current/sql-select.html#SQL-FOR-UPDATE-SHARE
