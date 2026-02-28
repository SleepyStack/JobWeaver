CREATE TABLE job_executions (
                                job_id       UUID PRIMARY KEY,
                                trace_id     VARCHAR(100) NOT NULL,
                                instruction JSONB NOT NULL,
                                job_status   VARCHAR(30) NOT NULL,
                                retry_count  INTEGER NOT NULL DEFAULT 0,
                                max_retries  INTEGER NOT NULL,
                                next_run_at  TIMESTAMPTZ NOT NULL,
                                last_error   TEXT,
                                updated_at   TIMESTAMPTZ NOT NULL,
                                version      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_job_executions_status
    ON job_executions (job_status);

CREATE INDEX idx_job_executions_next_run
    ON job_executions (next_run_at);

CREATE INDEX idx_job_executions_trace_id
    ON job_executions (trace_id);

ALTER TABLE job_executions
    ADD CONSTRAINT chk_retry_count_non_negative
        CHECK (retry_count >= 0);

ALTER TABLE job_executions
    ADD CONSTRAINT chk_max_retries_non_negative
        CHECK (max_retries >= 0);

ALTER TABLE job_executions
    ADD CONSTRAINT chk_job_status
        CHECK (job_status IN ('PENDING', 'RUNNING', 'FAILED', 'COMPLETED'));