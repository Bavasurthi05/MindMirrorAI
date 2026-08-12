-- Audit trail for retraining.
--
-- Changing the deployed model changes what every user is told about their own mind, so
-- who triggered a run, what it was trained on, how it scored, and whether it was
-- promoted all need to be answerable after the fact.

CREATE TABLE IF NOT EXISTS model_training_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id VARCHAR(64),
    version VARCHAR(64),
    status VARCHAR(24) NOT NULL,
    triggered_by VARCHAR(255) NOT NULL,
    corpus_total INT NOT NULL DEFAULT 0,
    corpus_user_examples INT NOT NULL DEFAULT 0,
    accuracy DOUBLE,
    f1_macro DOUBLE,
    gate_passed BOOLEAN NOT NULL DEFAULT FALSE,
    gate_detail TEXT,
    promoted BOOLEAN NOT NULL DEFAULT FALSE,
    promoted_at TIMESTAMP(6) NULL,
    promoted_by VARCHAR(255),
    forced BOOLEAN NOT NULL DEFAULT FALSE,
    message VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_training_run_created ON model_training_runs (created_at DESC);
CREATE INDEX idx_training_run_job ON model_training_runs (job_id);
