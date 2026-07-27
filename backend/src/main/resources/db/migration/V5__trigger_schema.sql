-- Trigger tracking
CREATE TABLE IF NOT EXISTS trigger_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(80) NOT NULL,
    intensity INT NOT NULL,
    note TEXT,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_trigger_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_trigger_user_occurred ON trigger_entries (user_id, occurred_at);
