-- Questionnaire / self-assessment submissions
CREATE TABLE IF NOT EXISTS assessment_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    questionnaire_key VARCHAR(50) NOT NULL,
    total_score INT NOT NULL,
    max_score INT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    answers TEXT,
    submitted_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_assessment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_assessment_user_submitted ON assessment_submissions (user_id, submitted_at);
