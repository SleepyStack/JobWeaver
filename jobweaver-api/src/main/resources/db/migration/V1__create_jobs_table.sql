CREATE TABLE jobs (
                      id BIGSERIAL PRIMARY KEY,
                      status VARCHAR(20) NOT NULL,
                      payload TEXT,
                      retry_count INT DEFAULT 0,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
