CREATE TABLE execution_attempts (
                                    event_id      UUID PRIMARY KEY,
                                    job_id        UUID NOT NULL,
                                    trace_id      VARCHAR(64) NOT NULL,
                                    started_at    TIMESTAMPTZ NOT NULL,
                                    finished_at   TIMESTAMPTZ,
                                    outcome       VARCHAR(20),
                                    error_message TEXT
);

CREATE INDEX idx_execution_attempts_job_id
    ON execution_attempts (job_id);

CREATE INDEX idx_execution_attempts_started_at
    ON execution_attempts (started_at);

ALTER TABLE execution_attempts
    ADD CONSTRAINT chk_execution_outcome
        CHECK (outcome IN ('RUNNING', 'SUCCESS', 'FAILURE'));