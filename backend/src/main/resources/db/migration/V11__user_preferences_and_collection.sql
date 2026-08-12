-- Deliberate data collection: preferences, onboarding, prompts and trigger confirmation.
--
-- Nothing in the app previously prompted users for mood or triggers, so analytics was empty
-- for almost everyone. These tables/columns back the check-in, onboarding and confirmation
-- surfaces that actually gather the data.

CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_time VARCHAR(5) NOT NULL DEFAULT '20:00',
    focus_areas VARCHAR(512),
    -- Separate, explicit, default-off consent for using this user's text as training data.
    -- Analysis for the user's own benefit never depends on this flag.
    training_consent BOOLEAN NOT NULL DEFAULT FALSE,
    training_consent_at TIMESTAMP(6) NULL,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    onboarding_step INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_user_preferences_user ON user_preferences (user_id);

-- Which prompt produced an entry, so we can later see which prompts yield the richest signal.
ALTER TABLE journal_entries ADD COLUMN prompt_id VARCHAR(64) NULL;

-- Confirmation state for auto-detected triggers. Confirmations and dismissals are both
-- high-quality signal for tuning the trigger lexicon.
ALTER TABLE trigger_entries ADD COLUMN confirmation VARCHAR(16) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE trigger_entries ADD COLUMN analysis_result_id BIGINT NULL;

CREATE INDEX idx_trigger_confirmation ON trigger_entries (user_id, confirmation);

-- Derived triggers written before this migration have never been reviewed by the user.
UPDATE trigger_entries SET confirmation = 'PENDING' WHERE source = 'DERIVED';
