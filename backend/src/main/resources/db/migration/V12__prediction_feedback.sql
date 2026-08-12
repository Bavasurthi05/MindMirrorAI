-- User corrections of model predictions.
--
-- This is the highest-quality label source available: a direct, supervised signal from the
-- person the prediction is about. It is what a future retraining pipeline learns from.

CREATE TABLE IF NOT EXISTS prediction_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    analysis_result_id BIGINT NOT NULL,
    predicted_label VARCHAR(32),
    corrected_label VARCHAR(32),
    agreement VARCHAR(16) NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_prediction_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_prediction_feedback_analysis FOREIGN KEY (analysis_result_id)
        REFERENCES analysis_results(id) ON DELETE CASCADE
);

CREATE INDEX idx_prediction_feedback_user ON prediction_feedback (user_id, created_at);
CREATE INDEX idx_prediction_feedback_agreement ON prediction_feedback (agreement);

-- One verdict per analysis: re-rating replaces the previous answer rather than stacking.
CREATE UNIQUE INDEX uk_prediction_feedback_analysis ON prediction_feedback (analysis_result_id);
