-- Persisted ML analysis results.
--
-- Before this migration the ML service was a dead-end call: results were returned
-- to the browser and discarded. Every analysis is now stored so the dashboard,
-- analytics, mirror and reports can be built from real signals.

CREATE TABLE IF NOT EXISTS analysis_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(24) NOT NULL,
    source_id BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(512),
    source_text TEXT,
    sentiment VARCHAR(24),
    sentiment_score DOUBLE,
    dominant_emotion VARCHAR(48),
    emotion_scores JSON,
    prediction VARCHAR(32),
    prediction_confidence DOUBLE,
    prediction_probabilities JSON,
    reasons JSON,
    explanation JSON,
    detected_triggers JSON,
    model_backend VARCHAR(48),
    model_version VARCHAR(64),
    analyzed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_analysis_result_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_analysis_result_user_time ON analysis_results (user_id, analyzed_at DESC);
CREATE INDEX idx_analysis_result_user_source ON analysis_results (user_id, source_type);
CREATE INDEX idx_analysis_result_status ON analysis_results (status);

-- Distinguish what the user told us from what we inferred, so a self-reported
-- check-in always outranks a derived one and analytics can filter either way.
ALTER TABLE mood_entries ADD COLUMN source VARCHAR(24) NOT NULL DEFAULT 'SELF_REPORTED';
ALTER TABLE trigger_entries ADD COLUMN source VARCHAR(24) NOT NULL DEFAULT 'USER_LOGGED';
