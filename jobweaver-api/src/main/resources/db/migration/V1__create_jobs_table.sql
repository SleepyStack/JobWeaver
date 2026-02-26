CREATE TABLE jobs (
                      id UUID PRIMARY KEY,
                      type VARCHAR(50),
                      payload JSONB,
                      status VARCHAR(50),
                      trace_id UUID,
                      retry_count INT DEFAULT 0,
                      max_retries INT DEFAULT 0,
                      worker_id VARCHAR(255),
                      last_error TEXT,
                      created_at TIMESTAMP,
                      updated_at TIMESTAMP,
                      started_at TIMESTAMP,
                      completed_at TIMESTAMP,
                      cancel_requested BOOLEAN DEFAULT FALSE,
                      cancelled BOOLEAN DEFAULT FALSE
);