# 📊 JobWeaver — Architecture Status & Next Steps

> Last updated: 2026-02-23

---

## Current Architecture State

### What Exists ✅

| Layer | Status | Details |
|---|---|---|
| **Infrastructure** | ✅ Complete | `docker-compose.yml` — Postgres 16, Zookeeper, Kafka (Confluent 7.6.1), healthchecks |
| **Parent POM** | ✅ Complete | Multi-module Maven project, Java 21, 4 modules declared |
| **`jobweaver-common`** | ✅ Minimal | `JobStatus` enum (`QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`), `JobMessage` DTO (`jobId`, `payload`) |
| **DB Schema** | ✅ Minimal | Flyway `V1` migration: `jobs` table with `id`, `status`, `payload`, `retry_count`, `created_at`, `updated_at` |
| **`jobweaver-api`** | ✅ MVP slice | `POST /api/jobs` persists a `QUEUED` job and publishes `JobCreatedEvent(jobId)` to Kafka (`job-execution-topic`) |
| **`jobweaver-worker`** | ✅ MVP slice | `@KafkaListener` consumes `JobCreatedEvent`, claims job in DB, executes payload instructions, updates status (`RUNNING` → `SUCCESS/FAILED`) |
| **`jobweaver-scheduler`** | ⚠️ Shell only | Spring Boot app only (no scheduling/retry logic yet) |

### What Doesn't Exist Yet ❌

- No job query API (`GET /api/jobs/{id}`, `GET /api/jobs`) yet
- No scheduler-based retry/backoff logic yet
- No dashboard integration yet (UI not wired to API)
- No business-logic tests yet

---

## 🎯 Next Milestone: Job Query + Visibility

**Goal:** Allow users to *see* what happened: query job status and surface it in logs/UI.

The submit/consume pipeline now exists; the next value is operational visibility (status query + basic dashboard wiring).

### Why this comes first

1. **Proves infrastructure** — Immediately validates that Kafka serialization, Postgres persistence, and service wiring all work together.
2. **Forces design decisions** — Defines the JPA entity, the API contract, and the Kafka topic name — all foundational decisions.
3. **Enables incremental testing** — Once this works, every future feature (status updates, retries, parallelism) can be tested against this pipeline.
4. **Avoids big-bang integration** — Building layers in isolation without connecting them won't reveal serialization mismatches or config errors until everything is wired.

### Next Files to Create (suggested)

**`jobweaver-api` module**:

1. `JobQueryController.java` — `GET /api/jobs/{id}`
2. `JobResponse.java` — response DTO
3. (Optional) list endpoint for recent jobs

### Current Data Flow (implemented)

```
POST /api/jobs {"jobType":"CPU_TASK","payload":{...},"maxRetryCount":0}
        │
        ▼
   JobController
        │
        ▼
   JobService.submitJob()
        ├── save Job(status=QUEUED) → Postgres
        └── kafkaTemplate.send("job-execution-topic", JobCreatedEvent) → Kafka
                                                                   │
                                                                   ▼
                                                          WorkerService.listen()
                                                               │
                                                               ▼
                                                          JobProcessor.process()
                                                               │
                                                               ▼
                                                          DB status updates + execution logs
```

### ⚠️ Common Mistakes to Avoid at This Stage

- **Don't add status updates in the worker yet** — keep it log-only. Wire status updates (`QUEUED` → `RUNNING` → `SUCCESS`) as a *separate* milestone so you can test the basic pipeline first.
- **Don't add error handling / retries yet** — that's milestone 3+.
- **Don't create a generic `TaskExecutor` abstraction yet** — premature. A simple log line is the right "executor" for now.

---

## Future Milestones (Roadmap)

| # | Milestone | Key Concepts |
|---|---|---|
| 1 | ✅ Infrastructure setup | Docker, Postgres, Kafka |
| 2 | ✅ **Submit & Consume vertical slice** | REST → DB → Kafka → Worker |
| 3 | Worker status updates | `QUEUED` → `RUNNING` → `SUCCESS/FAILED` lifecycle |
| 4 | Job query API | `GET /api/jobs/{id}`, `GET /api/jobs` with filters |
| 5 | Thread pool in worker | Concurrent job processing, `@Async` or `ExecutorService` |
| 6 | Idempotency guard | Prevent duplicate processing on Kafka redelivery |
| 7 | Scheduler service | Poll for `FAILED` jobs, retry with backoff |
| 8 | Observability | Structured logging, Actuator metrics, Kafka lag monitoring |
| 9 | Dashboard | React frontend for job status visualization |
