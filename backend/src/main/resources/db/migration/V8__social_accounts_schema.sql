-- Social account connections

CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    external_account_id VARCHAR(160) NOT NULL,
    display_name VARCHAR(160),
    profile_image_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'CONNECTED',
    connected_at TIMESTAMP(6),
    last_synced_at TIMESTAMP(6),
    oauth_scope VARCHAR(512),
    token_reference VARCHAR(1024),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_social_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_social_account_user ON social_accounts (user_id);
CREATE UNIQUE INDEX uk_social_account_identity ON social_accounts (user_id, provider, external_account_id);
