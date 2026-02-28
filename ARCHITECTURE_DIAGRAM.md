# JobWeaver -- Architecture Diagrams

> Mermaid-based diagrams illustrating the end-to-end request flow, service interactions, and internal component structures.

---

## 1. High-Level System Architecture

```mermaid
graph TB
    Client["Client Application"]

    subgraph API["jobweaver-api :8080"]
        direction TB
        MdcFilter["MDC Filter<br/>(traceId injection)"]
        Controller["JobController<br/>/api/jobs"]
        Service["JobService"]
        ApiDB[("PostgreSQL<br/>jobweaver_api<br/>:5432")]
        Publisher["JobEventPublisher"]
    end

    subgraph Kafka["Apache Kafka"]
        direction TB
        TopicCreated["job-created<br/>(1 partition)"]
        TopicRun["run-job<br/>(3 partitions)"]
        TopicCompleted["job-completed<br/>(3 partitions)"]
        TopicFailed["job-failed<br/>(3 partitions)"]
        TopicDLQ["job-dead-letter<br/>(1 partition)"]
    end

    subgraph Scheduler["jobweaver-scheduler :8081"]
        direction TB
        CreatedListener["JobCreatedListener"]
        Ingestion["IngestionService"]
        SchedulerDB[("PostgreSQL<br/>jobweaver_scheduler<br/>:5433")]
        DispatchScheduler["DispatchScheduler<br/>(@Scheduled 10s)"]
        DispatchService["JobDispatchService"]
        SchedulerSvc["SchedulerService"]
        CompletedListener["JobCompletedListener"]
        FailedListener["JobFailedListener"]
        RunPublisher["RunJobPublisher"]
        DLQPublisher["DeadLetterQueuePublisher"]
    end

    subgraph Worker["jobweaver-worker :8082"]
        direction TB
        RunListener["RunJobListener<br/>(3 consumers)"]
        ThreadPool["Thread Pool<br/>(12 threads)"]
        WorkerSvc["WorkerService"]
        AttemptProcessor["ExecutionAttemptProcessor"]
        SimExecutor["SimulationExecutor"]
        WorkerDB[("PostgreSQL<br/>jobweaver_worker<br/>:5434")]
        WorkerPublisher["WorkerEventPublisher"]
        OffsetMgr["OffsetCommitCoordinator"]
    end

    Client -->|"POST /api/jobs"| MdcFilter
    MdcFilter --> Controller
    Controller --> Service
    Service --> ApiDB
    Service --> Publisher
    Publisher -->|"sync send"| TopicCreated

    TopicCreated --> CreatedListener
    CreatedListener --> Ingestion
    Ingestion --> SchedulerDB

    DispatchScheduler -->|"poll every 10s"| DispatchService
    DispatchService -->|"SELECT ... FOR UPDATE SKIP LOCKED"| SchedulerDB
    DispatchService --> RunPublisher
    RunPublisher -->|"async send"| TopicRun

    TopicRun --> RunListener
    RunListener -->|"submit"| ThreadPool
    ThreadPool --> WorkerSvc
    WorkerSvc --> AttemptProcessor
    AttemptProcessor --> SimExecutor
    AttemptProcessor --> WorkerDB
    WorkerSvc --> WorkerPublisher
    RunListener --> OffsetMgr

    WorkerPublisher -->|"sync send"| TopicCompleted
    WorkerPublisher -->|"sync send"| TopicFailed

    TopicCompleted --> CompletedListener
    CompletedListener --> SchedulerSvc
    SchedulerSvc --> SchedulerDB

    TopicFailed --> FailedListener
    FailedListener --> SchedulerSvc
    SchedulerSvc -->|"retries exhausted"| DLQPublisher
    DLQPublisher --> TopicDLQ

    Client -->|"GET /api/jobs/{id}"| MdcFilter

    style API fill:#e8f4fd,stroke:#2196F3,stroke-width:2px
    style Scheduler fill:#fff3e0,stroke:#FF9800,stroke-width:2px
    style Worker fill:#e8f5e9,stroke:#4CAF50,stroke-width:2px
    style Kafka fill:#fce4ec,stroke:#E91E63,stroke-width:2px
```

---

## 2. End-to-End Request Flow (Sequence)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant API as jobweaver-api
    participant DB_A as PostgreSQL (API)
    participant K as Kafka
    participant SCH as jobweaver-scheduler
    participant DB_S as PostgreSQL (Scheduler)
    participant W as jobweaver-worker
    participant DB_W as PostgreSQL (Worker)

    C->>API: POST /api/jobs<br/>{jobType, payload, maxRetryCount}
    activate API
    API->>API: MdcFilter assigns traceId
    API->>API: JobService validates request
    API->>DB_A: INSERT INTO jobs (...)
    DB_A-->>API: Job persisted
    API->>K: Publish JobCreatedEvent<br/>topic: job-created (sync)
    K-->>API: Acknowledged
    API-->>C: 202 Accepted {jobId, traceId}
    deactivate API

    K->>SCH: JobCreatedEvent consumed
    activate SCH
    SCH->>DB_S: INSERT INTO job_executions<br/>(PENDING, retryCount=0)
    SCH-->>K: Manual ACK
    deactivate SCH

    Note over SCH: DispatchScheduler fires every 10 seconds

    activate SCH
    SCH->>DB_S: SELECT ... WHERE status=PENDING<br/>AND next_run_at <= now<br/>FOR UPDATE SKIP LOCKED
    DB_S-->>SCH: Eligible jobs (up to 50)
    SCH->>DB_S: UPDATE status = RUNNING
    SCH->>K: Publish RunJobEvent<br/>topic: run-job (async)
    deactivate SCH

    K->>W: RunJobEvent consumed
    activate W
    W->>W: Extract traceId, eventId from headers
    W->>W: Register offset in-flight
    W->>W: Submit to thread pool

    W->>DB_W: Check eventId exists (idempotency)
    W->>DB_W: INSERT execution_attempt (RUNNING)
    W->>W: SimulationExecutor runs steps
    W->>DB_W: UPDATE outcome = SUCCESS/FAILURE

    alt Job Succeeded
        W->>K: Publish JobCompletedEvent<br/>topic: job-completed (sync)
    else Job Failed
        W->>K: Publish JobFailedEvent<br/>topic: job-failed (sync)
    end
    W->>W: Mark offset completed
    deactivate W

    alt Completion
        K->>SCH: JobCompletedEvent consumed
        activate SCH
        SCH->>DB_S: UPDATE status = COMPLETED
        deactivate SCH
    else Failure with retries remaining
        K->>SCH: JobFailedEvent consumed
        activate SCH
        SCH->>DB_S: INCREMENT retryCount<br/>SET status = PENDING<br/>SET next_run_at = now + backoff
        Note over SCH: Job re-enters dispatch<br/>on next scheduler tick
        deactivate SCH
    else Failure with retries exhausted
        K->>SCH: JobFailedEvent consumed
        activate SCH
        SCH->>DB_S: UPDATE status = FAILED
        SCH->>K: Publish DeadLetterEvent<br/>topic: job-dead-letter
        deactivate SCH
    end
```

---

## 3. Kafka Topic Flow

```mermaid
graph LR
    API["jobweaver-api"]
    SCH["jobweaver-scheduler"]
    W["jobweaver-worker"]

    API -->|"JobCreatedEvent"| T1["job-created<br/>1 partition"]
    T1 -->|"consume"| SCH

    SCH -->|"RunJobEvent"| T2["run-job<br/>3 partitions"]
    T2 -->|"consume"| W

    W -->|"JobCompletedEvent"| T3["job-completed<br/>3 partitions"]
    T3 -->|"consume"| SCH

    W -->|"JobFailedEvent"| T4["job-failed<br/>3 partitions"]
    T4 -->|"consume"| SCH

    SCH -->|"DeadLetterEvent"| T5["job-dead-letter<br/>1 partition"]

    style T1 fill:#ffcdd2,stroke:#c62828
    style T2 fill:#ffcdd2,stroke:#c62828
    style T3 fill:#c8e6c9,stroke:#2e7d32
    style T4 fill:#ffcdd2,stroke:#c62828
    style T5 fill:#e0e0e0,stroke:#616161
    style API fill:#e8f4fd,stroke:#2196F3,stroke-width:2px
    style SCH fill:#fff3e0,stroke:#FF9800,stroke-width:2px
    style W fill:#e8f5e9,stroke:#4CAF50,stroke-width:2px
```

---

## 4. Job State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING: JobCreatedEvent received

    PENDING --> RUNNING: Dispatch scheduler picks up job<br/>(next_run_at elapsed)

    RUNNING --> COMPLETED: JobCompletedEvent received

    RUNNING --> PENDING: JobFailedEvent received<br/>(retries remaining)<br/>exponential backoff applied

    RUNNING --> FAILED: JobFailedEvent received<br/>(retries exhausted)<br/>DeadLetterEvent published

    COMPLETED --> [*]
    FAILED --> [*]
```

---

## 5. Worker Thread Model

```mermaid
graph TB
    subgraph KafkaConsumers["Kafka Consumer Threads (3)"]
        C1["Consumer Thread 1<br/>Partition 0"]
        C2["Consumer Thread 2<br/>Partition 1"]
        C3["Consumer Thread 3<br/>Partition 2"]
    end

    subgraph Pool["Thread Pool (12 threads, queue: 100)"]
        T1["job-processor-0"]
        T2["job-processor-1"]
        T3["job-processor-2"]
        T4["..."]
        T12["job-processor-11"]
    end

    subgraph Pipeline["Processing Pipeline"]
        IdempCheck["Idempotency Check<br/>(eventId as PK)"]
        Persist["Persist ExecutionAttempt<br/>(RUNNING)"]
        Execute["SimulationExecutor<br/>(step-by-step)"]
        Outcome["Mark SUCCESS / FAILURE"]
        Publish["Publish Outcome Event"]
    end

    subgraph OffsetMgmt["Async Offset Management"]
        PS["PartitionState<br/>(inFlight + completed)"]
        OCC["OffsetCommitCoordinator<br/>(contiguous watermark)"]
        RBH["ConsumerRebalanceHandler<br/>(flush on revoke)"]
    end

    C1 -->|"submit job"| Pool
    C2 -->|"submit job"| Pool
    C3 -->|"submit job"| Pool

    Pool --> IdempCheck
    IdempCheck --> Persist
    Persist --> Execute
    Execute --> Outcome
    Outcome --> Publish

    C1 -->|"register in-flight"| PS
    C2 -->|"register in-flight"| PS
    C3 -->|"register in-flight"| PS
    Pool -->|"mark completed"| PS
    C1 -->|"piggyback commit"| OCC
    C2 -->|"piggyback commit"| OCC
    C3 -->|"piggyback commit"| OCC
    OCC --> PS

    Note1["CallerRunsPolicy:<br/>if pool full, consumer<br/>thread runs job directly<br/>(natural back-pressure)"]

    style KafkaConsumers fill:#e8f4fd,stroke:#2196F3,stroke-width:2px
    style Pool fill:#fff3e0,stroke:#FF9800,stroke-width:2px
    style Pipeline fill:#e8f5e9,stroke:#4CAF50,stroke-width:2px
    style OffsetMgmt fill:#f3e5f5,stroke:#9C27B0,stroke-width:2px
    style Note1 fill:#fffde7,stroke:#f9a825,stroke-dasharray: 5 5
```

---

## 6. Retry and Backoff Flow

```mermaid
graph TD
    Fail["JobFailedEvent received"]
    Check{"retryCount<br/>< maxRetries?"}
    Backoff["Compute backoff delay<br/>min(5 * 2^retryCount, 300)s"]
    Schedule["Set status = PENDING<br/>Set next_run_at = now + delay<br/>Increment retryCount"]
    Dispatch["DispatchScheduler picks up<br/>on next tick (10s interval)"]
    DLQ["Mark status = FAILED<br/>Publish DeadLetterEvent"]

    Fail --> Check
    Check -->|"Yes"| Backoff
    Backoff --> Schedule
    Schedule --> Dispatch
    Dispatch -->|"Re-enters execution cycle"| Fail

    Check -->|"No"| DLQ

    style Fail fill:#ffcdd2,stroke:#c62828
    style Check fill:#fff9c4,stroke:#f57f17
    style Backoff fill:#fff3e0,stroke:#FF9800
    style Schedule fill:#e8f5e9,stroke:#4CAF50
    style Dispatch fill:#e8f4fd,stroke:#2196F3
    style DLQ fill:#e0e0e0,stroke:#616161
```

---

## 7. Database-per-Service Layout

```mermaid
graph TB
    subgraph API_DB["PostgreSQL :5432"]
        DB1[("jobweaver_api")]
        T1["jobs<br/>id, type, instruction,<br/>trace_id, created_at, updated_at"]
    end

    subgraph SCH_DB["PostgreSQL :5433"]
        DB2[("jobweaver_scheduler")]
        T2["job_executions<br/>job_id, trace_id, instruction,<br/>job_status, retry_count, max_retries,<br/>next_run_at, last_error, version"]
    end

    subgraph W_DB["PostgreSQL :5434"]
        DB3[("jobweaver_worker")]
        T3["execution_attempts<br/>event_id, job_id, trace_id,<br/>started_at, finished_at,<br/>outcome, error_message"]
    end

    API["jobweaver-api"] --> DB1
    SCH["jobweaver-scheduler"] --> DB2
    W["jobweaver-worker"] --> DB3

    DB1 -.-|"No direct access"| DB2
    DB2 -.-|"No direct access"| DB3

    Note["Services communicate<br/>exclusively via Kafka"]

    style API_DB fill:#e8f4fd,stroke:#2196F3,stroke-width:2px
    style SCH_DB fill:#fff3e0,stroke:#FF9800,stroke-width:2px
    style W_DB fill:#e8f5e9,stroke:#4CAF50,stroke-width:2px
    style Note fill:#fffde7,stroke:#f9a825,stroke-dasharray: 5 5
```

---

## 8. Docker Compose Infrastructure

```mermaid
graph TB
    subgraph Infrastructure["Infrastructure Layer"]
        ZK["Zookeeper<br/>:2181"]
        KF["Kafka<br/>:9092 / :29092"]
        PG1["postgres-api<br/>:5432"]
        PG2["postgres-scheduler<br/>:5433"]
        PG3["postgres-worker<br/>:5434"]
    end

    subgraph Applications["Application Layer"]
        API["jobweaver-api<br/>:8080<br/>eclipse-temurin:21-jre-alpine"]
        SCH["jobweaver-scheduler<br/>:8081<br/>eclipse-temurin:21-jre-alpine"]
        WRK["jobweaver-worker<br/>:8082<br/>eclipse-temurin:21-jre-alpine"]
    end

    ZK --> KF

    API -->|"depends_on"| PG1
    API -->|"depends_on"| KF
    SCH -->|"depends_on"| PG2
    SCH -->|"depends_on"| KF
    WRK -->|"depends_on"| PG3
    WRK -->|"depends_on"| KF

    PG1 -.->|"healthcheck:<br/>pg_isready"| PG1
    PG2 -.->|"healthcheck:<br/>pg_isready"| PG2
    PG3 -.->|"healthcheck:<br/>pg_isready"| PG3
    KF -.->|"healthcheck:<br/>kafka-topics --list"| KF
    API -.->|"healthcheck:<br/>/actuator/health"| API
    SCH -.->|"healthcheck:<br/>/actuator/health"| SCH
    WRK -.->|"healthcheck:<br/>/actuator/health"| WRK

    style Infrastructure fill:#f5f5f5,stroke:#9e9e9e,stroke-width:2px
    style Applications fill:#e8f5e9,stroke:#4CAF50,stroke-width:2px
```
