📊 JobWeaver — Architecture Status (Stage 2)

> Last updated: 2026-02-24

---

## A. Stage 1 Recap

### What Is Implemented

| Layer | Status | Details |
|---|---|---|
| **Infrastructure** | ✅ Complete | `docker-compose.yml` — Postgres 16, Zookeeper, Kafka (Confluent 7.6.1), healthchecks |
| **Parent POM** | ✅ Complete | Multi-module Maven project, Java 21, 4 modules declared |
| **`jobweaver-common`** | ✅ Minimal | `JobStatus` enum (`QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`), `JobMessage` DTO, shared exceptions |
| **DB Schema** | ✅ Minimal | Flyway `V1`: `jobs` table — `id`, `type`, `status`, `payload`, `retry_count`, `max_retries`, `worker_id`, `last_error`, `started_at`, `completed_at`, `created_at`, `updated_at`, `cancel_requested`, `cancelled` |
| **`jobweaver-api`** | ✅ MVP | `POST /api/jobs` persists a `QUEUED` job and publishes `JobCreatedEvent(jobId)` to `job-execution-topic` |
| **`jobweaver-worker`** | ✅ MVP | `@KafkaListener` (concurrency=3) consumes `JobCreatedEvent`, dispatches to `ExecutorService` (12 threads), claims job in DB, executes payload steps, marks `SUCCESS` or `FAILED` |
| **`jobweaver-scheduler`** | ⚠️ Shell only | Spring Boot bootstrap only — no logic |

### Stage 1 Data Flow

```
POST /api/jobs
       │
       ▼
  JobController
       │
       ▼
  JobService.submitJob()
       ├── INSERT jobs (status=QUEUED) → Postgres
       └── kafkaTemplate.send("job-execution-topic", JobCreatedEvent{jobId}) → Kafka
                                                                   │
                                                         WorkerService (@KafkaListener)
                                                                   │
                                                         ExecutorService (thread pool)
                                                                   │
                                                         JobProcessor.process()
                                                                   │
                                                         UPDATE jobs status RUNNING
                                                                   │
                                                         InstructionExecutor.execute()
                                                                   │
                                                         UPDATE jobs status SUCCESS / FAILED
```

### Current Limitations

1. **Scheduler is unused** — no scope or contract defined.
2. **No idempotency** — Kafka delivers at-least-once; redelivery causes duplicate job execution.
3. **No retry policy** — `FAILED` jobs stay failed permanently; `retry_count`/`max_retries` columns exist but are not enforced.
4. **No DLQ** — unrecoverable or max-retried jobs have nowhere to go.
5. **Offset acknowledgement is implicit** — auto-commit may commit before async execution completes, leading to lost work on crash.
6. **No job query API** — no `GET /api/jobs/{id}`; job status is not queryable.
7. **Minimal observability** — no structured log fields, no traceId, no metrics.
8. **Hardening not addressed** — no design for crash recovery, consumer rebalances, or poison messages.

---

## B. Stage 2 Request Flow and Job Lifecycle

### B.1 Identifier Strategy

Three distinct identifiers are used:

| Identifier | Type | Scope | Owner |
|---|---|---|---|
| `jobId` | UUID (DB primary key) | Identifies the job record | API assigns at submission |
| `traceId` | UUID (per HTTP request or per Kafka message) | Traces a single request chain across services | API generates per HTTP request; propagated through Kafka headers |
| `eventId` | UUID (per Kafka message) | Identifies one specific Kafka message instance | Producer assigns per `send()` call |

**Decision:** `jobId` alone is insufficient for full observability.
- Multiple Kafka messages may carry the same `jobId` (retries, redeliveries).
- `traceId` allows correlating all log lines for one client request end-to-end.
- `eventId` allows identifying which specific delivery a worker acted on.

All three must appear in logs and in Kafka message headers/payload.

### B.2 End-to-End Request Flow

#### Submission Flow (Client → API → DB → Kafka)

```
Client
  │  POST /api/jobs  {type, payload, maxRetries}
  ▼
JobController
  │  generates jobId (UUID), traceId (UUID)
  ▼
JobService.submitJob()
  ├── INSERT jobs (id=jobId, status=QUEUED, attempt=0) → Postgres   [1]
  └── kafkaTemplate.send(
        topic   = "job-execution-topic",
        key     = jobId,                                              [2]
        headers = {traceId, eventId},
        payload = JobCreatedEvent{jobId, traceId, eventId}
      )
  ▼
JobController returns 202 Accepted {jobId, traceId}
```

**Notes:**
- `[1]` DB write happens before Kafka publish. If Kafka publish fails, the job remains `QUEUED` in DB; the scheduler will eventually republish it (see C).
- `[2]` Using `jobId` as the Kafka partition key ensures all events for the same job go to the same partition, preserving ordering per job.

#### Execution Flow (Kafka → Worker → DB)

```
Kafka (job-execution-topic, partition P)
  │
WorkerService (@KafkaListener, manual ack, concurrency=3)
  │  extracts jobId, traceId, eventId from headers
  ▼
Idempotency check: is job status QUEUED?             [3]
  │  YES → proceed
  │  NO  → log "skip duplicate/stale event", ack offset, return
  ▼
ExecutorService.submit(JobProcessor::process)        [4]
  │  (listener thread acks offset AFTER submitting)  [5]
  ▼
JobProcessor.process(jobId, traceId, eventId)
  ├── DB: UPDATE status=RUNNING WHERE id=jobId AND status=QUEUED   [6]
  │       (atomic claim; concurrent workers compete via DB)
  │   0 rows updated → log "already claimed", return
  ├── InstructionExecutor.execute(steps)
  │   SUCCESS → UPDATE status=SUCCESS, completedAt=now
  │   RETRYABLE FAILURE → UPDATE status=RETRY_WAIT, nextRunAt=now+backoff, retry_count++   [7]
  │   TERMINAL FAILURE  → UPDATE status=FAILED, lastError=..., completedAt=now
  │                        publish to DLQ topic                    [8]
```

**Notes:**
- `[3]` Pre-check guards against stale redeliveries before submitting to thread pool.
- `[4]` Async dispatch — listener thread is freed immediately; see D for offset commit implications.
- `[5]` Offset is acked by the listener thread after submitting to the executor, not after completion. This is safe **only** with idempotency in place (a redelivered event after restart will be de-duplicated at `[3]`/`[6]`).
- `[6]` The conditional `UPDATE … WHERE status=QUEUED` is the atomic claim. Only one worker succeeds; others get 0 rows and skip.
- `[7]` `RETRY_WAIT` is a new status introduced in Stage 2 (see B.3).
- `[8]` DLQ publish happens inside the worker on terminal failure (see E).

#### Failure Flows

**Retryable failure:**
```
InstructionExecutor throws RetryableJobException
  │
JobProcessor
  ├── retry_count++ <= max_retries?
  │     YES → UPDATE status=RETRY_WAIT, retry_count=retry_count+1, nextRunAt=now+backoff(retry_count)
  │     NO  → UPDATE status=FAILED (max retries exhausted)
  │             publish JobFailedEvent to DLQ topic
```

**Terminal / non-retryable failure:**
```
InstructionExecutor throws NonRetryableJobException (or unknown exception)
  │
JobProcessor
  └── UPDATE status=FAILED, lastError=message, completedAt=now
       publish JobFailedEvent to DLQ topic
```

### B.3 Job State Machine

#### States

| State | Meaning |
|---|---|
| `QUEUED` | Accepted by API; waiting for a worker to claim it |
| `RUNNING` | Claimed by a worker; execution in progress |
| `RETRY_WAIT` | Execution failed with a retryable error; waiting for `nextRunAt` |
| `SUCCESS` | Execution completed successfully (terminal) |
| `FAILED` | Execution failed permanently (terminal — max retries exhausted or non-retryable) |

#### Transitions

```
               ┌──────────────────────────────────────────────────────────┐
               │                                                          │
   [submit]    │ [worker claims]   [execution ok]                        │
  ──────────► QUEUED ──────────► RUNNING ──────────────────────► SUCCESS │
               ▲                    │                                     │
               │                    │ [retryable fail,                   │
               │                    │  retry_count < max_retries]        │
               │                    ▼                                     │
               │              RETRY_WAIT                                 │
               │              (nextRunAt set)                            │
               │                    │                                     │
               │  [scheduler        │                                     │
               │   republishes]     │                                     │
               └────────────────────┘                                     │
                                    │ [non-retryable fail                 │
                                    │  OR retry_count >= max_retries]     │
                                    ▼                                     │
                                  FAILED ───────────────────────────────►┘
                                  (terminal; DLQ published)
```

#### Invariants

1. **Only one worker can claim a job** — the atomic `UPDATE … WHERE status=QUEUED` guarantees this.
2. **Transitions are monotonic** — a job never moves backward (e.g., `SUCCESS` → `QUEUED` is forbidden).
3. **Retry counter only increases** — `retry_count` increments on every `RETRY_WAIT` transition and is never reset.
4. **Terminal states are final** — `SUCCESS` and `FAILED` are absorbing states; no further transitions are permitted.
5. **`RUNNING` is a non-durable claim** — on worker crash, a job may remain stuck in `RUNNING`. The scheduler must detect and recover these (see C.4).

### B.4 Log and Event Fields

Every log line and Kafka event **must** include:

| Field | Source | Purpose |
|---|---|---|
| `jobId` | DB primary key | Correlates all activity for a job |
| `traceId` | Generated at API ingress | Correlates one end-to-end request chain |
| `eventId` | Generated per Kafka `send()` | Identifies a specific message instance |
| `attempt` | DB `retry_count` column (0-indexed: 0 = first attempt, 1 = first retry) | Disambiguates which execution attempt a log line belongs to |
| `workerId` | Worker hostname / pod ID | Identifies which worker instance processed the job |
| `status` | Job state at log time | Human-readable progress marker |

---

## C. Scheduler: Scope and Responsibilities

### C.1 What the Scheduler Owns

- **Retry republication** — polls DB for `RETRY_WAIT` jobs whose `nextRunAt <= now` and republishes them to `job-execution-topic`.
- **Stuck-`RUNNING` recovery** — detects jobs stuck in `RUNNING` for longer than a configurable timeout (e.g., `runningTimeoutMinutes`) and resets them to `QUEUED` (or `RETRY_WAIT`) for reprocessing.
- **Nothing else** — the scheduler does not execute jobs, does not consume from Kafka, and does not write job results.

### C.2 What the Scheduler Does NOT Own

- Job execution logic (owned by Worker).
- Kafka consumer group management (owned by Worker).
- DLQ publishing (owned by Worker at point of terminal failure).
- API concerns (owned by API).

### C.3 Recommended Model: DB-Driven Retries (Option 1)

**Chosen:** DB-driven retries via `RETRY_WAIT` + scheduler poll.

**Rationale:** Simpler operationally — the DB is the single source of truth for retry state; the scheduler is stateless and restartable without coordination. Kafka retry topics (Option 2) introduce additional topic management and delay mechanisms that are unnecessary at Stage 2 scale.

#### Scheduler Flow

```
Scheduler (fixed-rate poll, interval = 10s)
  │
  ├── Query 1 (retry republication):
  │     SELECT id FROM jobs
  │     WHERE status = 'RETRY_WAIT'
  │       AND next_run_at <= NOW()
  │     LIMIT 50
  │     FOR UPDATE SKIP LOCKED          ← prevents duplicate republish on multi-instance scheduler
  │
  │   For each row:
  │     UPDATE jobs SET status='QUEUED' WHERE id=? AND status='RETRY_WAIT'   ← atomic re-queue
  │     kafkaTemplate.send("job-execution-topic", JobCreatedEvent{jobId, newEventId, traceId})
  │
  └── Query 2 (stuck-RUNNING recovery):
        SELECT id FROM jobs
        WHERE status = 'RUNNING'
          AND started_at < NOW() - INTERVAL '<runningTimeoutMinutes> minutes'
        LIMIT 20
        FOR UPDATE SKIP LOCKED

      For each row:
        UPDATE jobs SET status='RETRY_WAIT', next_run_at=NOW() WHERE id=? AND status='RUNNING'
```

#### Configuration Parameters

| Parameter | Default | Description |
|---|---|---|
| `scheduler.poll-interval-ms` | 10000 | How often to poll for due retries |
| `scheduler.retry-batch-size` | 50 | Max jobs to republish per cycle |
| `scheduler.running-timeout-minutes` | 15 | Max time a job may stay in RUNNING before recovery |

### C.4 Scheduler Crash and Restart

- The scheduler is stateless — all state is in the DB.
- On restart it immediately resumes polling from the current time.
- `FOR UPDATE SKIP LOCKED` prevents two scheduler instances from republishing the same job concurrently (safe for rolling restarts or horizontal scale).
- A job in `QUEUED` state (already republished before crash) will be picked up naturally by the next worker poll; no double-republish occurs because status was already updated to `QUEUED` before the send.

### C.5 How Max Retries Are Enforced

```
Worker detects retryable failure:
  IF job.retry_count >= job.max_retries:
    UPDATE status=FAILED   ← scheduler will never see this job again
    publish to DLQ
  ELSE:
    UPDATE status=RETRY_WAIT, retry_count=retry_count+1, nextRunAt=now+backoff(retry_count)
```

The scheduler only republishes `RETRY_WAIT` jobs; it never changes `FAILED` jobs.

---

## D. Kafka Semantics & Offset Management

> This section is a research checklist and decision guide for implementation.

### D.1 Delivery Semantics

**Kafka is at-least-once by default.** This means:

- A message may be delivered more than once (on consumer restart, rebalance, or processing crash before offset commit).
- **Implication:** The worker must be idempotent. Processing the same `jobId` twice must not result in two executions. The atomic `UPDATE … WHERE status=QUEUED` claim in `JobProcessor` provides this guard.
- Exactly-once semantics (EOS) exist in Kafka but require transactional producers and are not warranted at Stage 2. Idempotent consumers are the preferred approach.

### D.2 Consumer Groups, Partitions, and Concurrency

| Parameter | Value | Notes |
|---|---|---|
| Topic partitions | 3 | All workers in `worker-group` share these partitions |
| Consumer concurrency | 3 | One listener thread per partition (matches partition count) |
| Thread pool | 12 | 3 listener threads × 4 executor threads each |
| Consumer group | `worker-group` | Only one group; no fan-out to other consumers at Stage 2 |

- Partition count caps the parallelism at the Kafka layer. Adding more worker instances beyond 3 does not increase Kafka consumer throughput for this topic.
- Each partition's messages are delivered in order to its assigned listener thread. Within a partition, messages for different jobs are interleaved; within a single job, all events are ordered (since `jobId` is the partition key).

### D.3 Offset Commit Strategies

**The core tension:** The current design dispatches job processing asynchronously to an `ExecutorService`. The listener thread submits the task and returns. Kafka does not know when the async task finishes.

#### Option A — Auto-commit (current implicit behavior)
- Offsets are committed by the framework on a timer (default 5 s) regardless of processing outcome.
- **Problem:** If the worker crashes after the auto-commit but before processing completes, the job is silently lost (offset already committed, no redelivery).
- **Verdict:** Not safe for async dispatch without idempotency.

#### Option B — Manual ack after submit (recommended for Stage 2)
- Set `enable.auto.commit=false`; use `AckMode.MANUAL_IMMEDIATE`.
- Listener thread calls `acknowledgment.acknowledge()` after submitting to the executor (not after completion).
- **Implication:** If the worker crashes between ack and task completion, Kafka redelivers the event. The worker processes it again. The atomic DB claim (`WHERE status=QUEUED`) de-duplicates the execution.
- **Why this is safe:** Idempotency is in the DB, not in Kafka. Committing early is acceptable only because duplicate delivery is handled explicitly.
- **Verdict:** Adopt this for Stage 2.

#### Option C — Manual ack after completion
- Listener thread blocks until the async task finishes, then acks.
- Preserves at-most-once execution without relying on DB idempotency.
- **Problem:** Blocks the listener thread (ties up the Kafka poll thread); reduces throughput; increases risk of consumer group rebalance due to poll timeout.
- **Verdict:** Do not use with async dispatch.

**Decision for Stage 2:** Use **Option B** (manual ack after submit) with DB-level idempotency.

### D.4 Poison Message Strategy

A poison message is one that always causes an exception before or during DB claim (e.g., malformed JSON, missing `jobId`, DB connection lost).

**Handling approach:**
1. Deserialize errors — configure a `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer` to send unparseable messages directly to `job-execution-topic.DLT` (dead letter topic).
2. Processing exceptions — catch all exceptions in `JobProcessor`; distinguish retryable from non-retryable; never let an uncaught exception propagate back to the listener (which would cause infinite redelivery by default).
3. After `maxFailures` delivery attempts for a single message (configurable), route to DLT automatically via Spring Kafka's `DefaultErrorHandler`.

### D.5 Ordering Guarantees

- Within a partition, Kafka guarantees message order.
- Since `jobId` is used as the partition key, all events for a single job land in the same partition in submission order.
- **Job-level ordering is therefore preserved** by Kafka for redelivery events.
- **Caveat:** Async dispatch to the thread pool does not preserve Kafka order within a partition. If ordering within a job matters (e.g., cancellation event must not be processed before the initial run), this must be handled at the application layer (status check in DB).

---

## E. Error Handling, Retries, and DLQ

### E.1 Error Classification

| Category | Examples | Action |
|---|---|---|
| **Retryable** | Transient DB error, temporary HTTP timeout, resource contention | Increment attempt, set `RETRY_WAIT`, schedule backoff |
| **Non-retryable** | Business rule violation, malformed payload, `FAIL` instruction | Immediately set `FAILED`, publish to DLQ |
| **Poison message** | Kafka deserialisation failure, missing `jobId` | Route via Spring Kafka `DeadLetterPublishingRecoverer` to DLT |

Use a dedicated `RetryableJobException` and `NonRetryableJobException` in `jobweaver-common`. Unknown `RuntimeException` should be treated as retryable by default (fail-safe).

### E.2 Backoff Strategy

**Exponential backoff with jitter** (avoids thundering herd on mass failure):

```
backoff(attempt) = min(baseDelay * 2^attempt, maxDelay) + random(0, jitter)
```

| Parameter | Recommended Value |
|---|---|
| `retry.base-delay-seconds` | 10 |
| `retry.max-delay-seconds` | 300 (5 min) |
| `retry.jitter-seconds` | 5 |
| `retry.max-attempts` | 5 (configurable per job via `max_retries` column) |

Example delays for `baseDelay=10`, `maxDelay=300`:

| Attempt | Delay before next try |
|---|---|
| 1 | ~10 s |
| 2 | ~20 s |
| 3 | ~40 s |
| 4 | ~80 s |
| 5 | ~160 s |

### E.3 DLQ Design

**DLQ Kafka topic:** `job-dlq-topic` (1 partition, retention = 7 days minimum)

**DLQ payload (`JobFailedEvent`):**

```json
{
  "jobId":        "uuid",
  "eventId":      "uuid of the originating Kafka event",
  "traceId":      "uuid of the request chain",
  "workerId":     "hostname or pod id of the worker",
  "attempt":      3,
  "maxAttempts":  5,
  "failureType":  "NON_RETRYABLE | MAX_RETRIES_EXHAUSTED",
  "errorSummary": "Short description of the last error",
  "failedAt":     "2026-02-24T16:00:00Z"
}
```

### E.4 DLQ Operational Actions

| Action | How |
|---|---|
| **Inspect** | Consume from `job-dlq-topic` with a CLI consumer or dashboard view |
| **Replay** | Reset job status to `QUEUED` in DB; publish a new `JobCreatedEvent` to `job-execution-topic` |
| **Mark terminal** | Leave in `FAILED`; update a `dlq_reviewed` flag in DB (future) |

---

## F. Observability & Operational Readiness

### F.1 Structured Logging Fields

Every log line emitted by API, Worker, or Scheduler **must** include these MDC fields:

```
jobId=<uuid>  traceId=<uuid>  eventId=<uuid>  attempt=<n>  workerId=<hostname>  status=<STATE>
```

Example (JSON log format):
```json
{
  "timestamp": "2026-02-24T16:00:00Z",
  "level": "INFO",
  "logger": "JobProcessor",
  "message": "Job execution started",
  "jobId": "a1b2c3d4-...",
  "traceId": "e5f6g7h8-...",
  "eventId": "i9j0k1l2-...",
  "attempt": 1,
  "workerId": "worker-host-01",
  "status": "RUNNING"
}
```

### F.2 Metrics (to implement in a future milestone)

| Metric | Type | Labels |
|---|---|---|
| `jobweaver.jobs.submitted` | Counter | `type` |
| `jobweaver.jobs.completed` | Counter | `type`, `status` (SUCCESS/FAILED) |
| `jobweaver.jobs.retried` | Counter | `type` |
| `jobweaver.jobs.dlq` | Counter | `type`, `failureType` |
| `jobweaver.jobs.duration.seconds` | Histogram | `type`, `status` |
| `jobweaver.worker.active.threads` | Gauge | `workerId` |
| `kafka.consumer.lag` | Gauge | `topic`, `partition`, `consumerGroup` |

Expose via Spring Boot Actuator + Micrometer.

### F.3 Config Hygiene

- **No hard-coded `localhost`** anywhere in application code. All connection strings must come from environment variables or `application.yml` with override support.
- Required env vars per service:

| Service | Variable | Example |
|---|---|---|
| All | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/jobweaver` |
| All | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` |
| Worker | `WORKER_ID` | Hostname / pod name |
| Scheduler | `SCHEDULER_POLL_INTERVAL_MS` | `10000` |

- Secrets (DB passwords) must be injected via environment variables and never committed to source control.

### F.4 Health Checks

Each service must expose Spring Boot Actuator endpoints:

| Endpoint | Purpose |
|---|---|
| `GET /actuator/health/liveness` | Process is alive (JVM running) |
| `GET /actuator/health/readiness` | Service is ready (DB + Kafka connections healthy) |

Readiness should check:
- DB connection pool reachability (`DataSourceHealthIndicator`)
- Kafka producer/consumer reachability (`KafkaHealthIndicator`)

---

## G. Stage 2 Definition of Done

The following criteria define when Stage 2 is complete and ready for production use.

### G.1 Functional Correctness

- [ ] `POST /api/jobs` returns 202 with `jobId` and `traceId`.
- [ ] Job transitions correctly through `QUEUED → RUNNING → SUCCESS` for a healthy payload.
- [ ] `GET /api/jobs/{id}` returns current job status and metadata.
- [ ] A forced transient failure (`FAIL` instruction with retryable flag) causes the job to enter `RETRY_WAIT`, and the scheduler republishes it within one polling cycle.
- [ ] After `maxRetries` exhausted, job moves to `FAILED` and a DLQ event is published to `job-dlq-topic`.
- [ ] A duplicate Kafka delivery (same event replayed) does not cause a second execution (idempotency).

### G.2 Scheduler Integration

- [ ] `jobweaver-scheduler` runs as part of `docker compose up`.
- [ ] Scheduler polls DB for `RETRY_WAIT` jobs and republishes within `poll-interval-ms`.
- [ ] Scheduler detects stuck-`RUNNING` jobs and resets them after `running-timeout-minutes`.
- [ ] Scheduler restart does not cause duplicate republication.

### G.3 Observability

- [ ] Every log line for a job includes `jobId`, `traceId`, `eventId`, `attempt`, `workerId`.
- [ ] A single job's lifecycle can be reconstructed end-to-end using `jobId` from submitted log to completion log.
- [ ] Spring Actuator liveness and readiness endpoints respond correctly.

### G.4 Resilience

- [ ] Worker crash mid-execution: job is recovered by scheduler stuck-`RUNNING` detection and reprocessed.
- [ ] Kafka consumer rebalance: no job is processed twice due to offset commit strategy.
- [ ] Poison message (malformed JSON): routed to `job-execution-topic.DLT` without blocking the consumer.

### G.5 Configuration

- [ ] No hard-coded `localhost` in application code.
- [ ] All services start cleanly with environment variable configuration.
- [ ] `docker-compose.yml` updated to run API, Worker, and Scheduler together with proper env vars.

---

## H. Implementation Roadmap

| # | Milestone | Key Deliverables |
|---|---|---|
| 1 | ✅ Infrastructure | Docker, Postgres, Kafka |
| 2 | ✅ Submit & Consume (Stage 1) | REST → DB → Kafka → Worker |
| 3 | Job query API | `GET /api/jobs/{id}`, `GET /api/jobs` |
| 4 | Idempotency guard | Atomic claim; `RETRY_WAIT` status; `attempt` tracking |
| 5 | Manual offset ack | `AckMode.MANUAL_IMMEDIATE`; ack after executor submit |
| 6 | Scheduler — retry republication | Poll `RETRY_WAIT`, `FOR UPDATE SKIP LOCKED`, republish |
| 7 | Scheduler — stuck recovery | Detect stale `RUNNING`; reset to `RETRY_WAIT` |
| 8 | DLQ | Terminal failure publishes to `job-dlq-topic`; `JobFailedEvent` payload |
| 9 | Structured logging | MDC fields: `jobId`, `traceId`, `eventId`, `attempt`, `workerId` |
| 10 | Observability | Actuator metrics, health endpoints, Kafka lag monitoring |
| 11 | Config hygiene | Env-driven config; no hard-coded localhost; Docker Compose wired |
| 12 | Dashboard integration | React frontend wired to job query API |
