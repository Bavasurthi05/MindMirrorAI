-- Social media posts imported from a user's own data export.
--
-- The official X / Facebook / Instagram APIs are unusable for this app: X's developer terms
-- forbid inferring health from its data, Meta only grants timeline access for memory books
-- and parental monitoring, and personal Instagram accounts have had no API since Dec 2024.
-- Users instead download their data archive and upload it here.

CREATE TABLE IF NOT EXISTS social_imports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(16) NOT NULL,
    original_filename VARCHAR(255),
    status VARCHAR(16) NOT NULL,
    posts_found INT NOT NULL DEFAULT 0,
    posts_imported INT NOT NULL DEFAULT 0,
    skipped_duplicates INT NOT NULL DEFAULT 0,
    skipped_reposts INT NOT NULL DEFAULT 0,
    skipped_short INT NOT NULL DEFAULT 0,
    skipped_over_limit INT NOT NULL DEFAULT 0,
    earliest_post_at TIMESTAMP(6) NULL,
    latest_post_at TIMESTAMP(6) NULL,
    error_message VARCHAR(512),
    -- When the user confirmed they understood what the upload is used for.
    acknowledged_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_social_import_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_social_import_user ON social_imports (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS social_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    import_id BIGINT NOT NULL,
    provider VARCHAR(16) NOT NULL,
    -- The platform's id where the export has one (X), otherwise a hash of date + text, so
    -- uploading the same archive twice does not duplicate posts.
    external_post_id VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    -- When the post was published, which is what the timeline is built from. Analysis time
    -- would put a five-year archive entirely on the day it was uploaded.
    posted_at TIMESTAMP(6) NULL,
    analysis_result_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_social_post_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_social_post_import FOREIGN KEY (import_id) REFERENCES social_imports(id) ON DELETE CASCADE,
    CONSTRAINT fk_social_post_analysis FOREIGN KEY (analysis_result_id)
        REFERENCES analysis_results(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uk_social_post_identity ON social_posts (user_id, provider, external_post_id);
CREATE INDEX idx_social_post_import ON social_posts (import_id);
CREATE INDEX idx_social_post_user_time ON social_posts (user_id, posted_at DESC);
