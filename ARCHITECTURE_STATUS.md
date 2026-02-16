# 📊 JobWeaver — Architecture Status & Next Steps

> Last updated: 2026-02-17

---

## Current Architecture State

### What Exists ✅

| Layer | Status | Details |
|---|---|---|
| **Infrastructure** | ✅ Complete | `docker-compose.yml` — Postgres 16, Zookeeper, Kafka (Confluent 7.6.1), healthchecks |
| **Parent POM** | ✅ Complete | Multi-module Maven project, Java 21, 4 modules declared |
| **`jobweaver-common`** | ✅ Minimal | `JobStatus` enum (`QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`), `JobMessage` DTO (`jobId`, `payload`) |
| **DB Schema** | ✅ Minimal | Flyway `V1` migration: `jobs` table with `id`, `status`, `payload`, `retry_count`, `created_at`, `updated_at` |
| **`jobweaver-api`** | ⚠️ Shell only | Has all dependencies (Spring Web, JPA, Kafka, Flyway, Actuator) but **zero business logic** — just `@SpringBootApplication` |
| **`jobweaver-worker`** | ⚠️ Shell only | Has dependencies + Kafka consumer config in YAML, but **no listener, no executor** |
| **`jobweaver-scheduler`** | ⚠️ Shell only | Same as worker — shell only, config copied from worker |

### What Doesn't Exist Yet ❌

- No JPA entity for `Job`
- No repository layer
- No REST controller
- No Kafka producer (API side)
- No Kafka consumer/listener (Worker side)
- No service layer anywhere
- No tests with business logic

---

## 🎯 Next Milestone: The "Submit & Consume" Vertical Slice

**Goal:** Submit a job via REST → persist it as `QUEUED` → publish to Kafka → worker picks it up → logs it.

This is the single highest-impact step because it **lights up the entire data path end-to-end** and proves the infrastructure works. Everything else (retries, thread pools, scheduler) layers on top of this working pipeline.

### Why this comes first

1. **Proves infrastructure** — Immediately validates that Kafka serialization, Postgres persistence, and service wiring all work together.
2. **Forces design decisions** — Defines the JPA entity, the API contract, and the Kafka topic name — all foundational decisions.
3. **Enables incremental testing** — Once this works, every future feature (status updates, retries, parallelism) can be tested against this pipeline.
4. **Avoids big-bang integration** — Building layers in isolation without connecting them won't reveal serialization mismatches or config errors until everything is wired.

### Files to Create (in order)

**`jobweaver-api` module** (5 files):

1. `Job.java` — JPA `@Entity` mapping to the `jobs` table
2. `JobRepository.java` — Spring Data JPA interface
3. `CreateJobRequest.java` — Incoming DTO (just `payload` for now)
4. `JobService.java` — Saves job as `QUEUED`, publishes `JobMessage` to Kafka
5. `JobController.java` — `POST /api/jobs` endpoint

**`jobweaver-worker` module** (1 file):

6. `JobConsumer.java` — `@KafkaListener` that logs the received `JobMessage`

### Expected Data Flow

```
POST /api/jobs {"payload": "send-email"}
        │
        ▼
   JobController
        │
        ▼
   JobService.submitJob()
        ├── save Job(status=QUEUED) → Postgres
        └── kafkaTemplate.send("job-submissions", JobMessage) → Kafka
                                                                   │
                                                                   ▼
                                                          JobConsumer.onMessage()
                                                               │
                                                               ▼
                                                          LOG: "Received job #42"
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
| 2 | 🔜 **Submit & Consume vertical slice** | REST → DB → Kafka → Worker |
| 3 | Worker status updates | `QUEUED` → `RUNNING` → `SUCCESS/FAILED` lifecycle |
| 4 | Job query API | `GET /api/jobs/{id}`, `GET /api/jobs` with filters |
| 5 | Thread pool in worker | Concurrent job processing, `@Async` or `ExecutorService` |
| 6 | Idempotency guard | Prevent duplicate processing on Kafka redelivery |
| 7 | Scheduler service | Poll for `FAILED` jobs, retry with backoff |
| 8 | Observability | Structured logging, Actuator metrics, Kafka lag monitoring |
| 9 | Dashboard | React frontend for job status visualization |
