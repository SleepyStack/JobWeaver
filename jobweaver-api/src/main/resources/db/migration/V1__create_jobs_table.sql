CREATE TABLE jobs (
                      id          UUID PRIMARY KEY,
                      type        VARCHAR(50) NOT NULL,
                      instruction JSONB NOT NULL,
                      trace_id    VARCHAR(100) NOT NULL,
                      created_at  TIMESTAMPTZ NOT NULL,
                      updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_jobs_trace_id
    ON jobs (trace_id);

CREATE INDEX idx_jobs_created_at
    ON jobs (created_at);