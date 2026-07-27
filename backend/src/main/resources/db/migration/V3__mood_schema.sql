-- Mood tracking
CREATE TABLE IF NOT EXISTS mood_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    mood_score INT NOT NULL,
    mood_label VARCHAR(50),
    note TEXT,
    recorded_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_mood_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_mood_user_recorded ON mood_entries (user_id, recorded_at);
