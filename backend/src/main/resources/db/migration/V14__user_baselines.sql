-- Per-user baselines for personalized interpretation.
--
-- A global classifier can say "this reads as stress". It cannot say whether that is
-- unusual *for this person*. These rows hold each user's own normal range so insights can
-- be expressed as deviation from their baseline rather than as a bare label.

CREATE TABLE IF NOT EXISTS user_baselines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sample_size INT NOT NULL DEFAULT 0,
    days_covered INT NOT NULL DEFAULT 0,
    mean_sentiment DOUBLE,
    stddev_sentiment DOUBLE,
    mean_mood DOUBLE,
    stddev_mood DOUBLE,
    mean_trigger_intensity DOUBLE,
    journaling_days_per_week DOUBLE,
    state_rates JSON,
    established BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_baseline_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_user_baseline_user ON user_baselines (user_id);
