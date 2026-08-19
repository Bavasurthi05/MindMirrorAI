-- Audit trail for privileged account changes.
--
-- Granting admin rights or disabling an account are the kind of actions someone will
-- eventually need to explain, so who did what to whom is recorded rather than only
-- appearing in application logs that rotate away.

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(32) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    target_user_id BIGINT,
    target_email VARCHAR(255),
    previous_value VARCHAR(64),
    new_value VARCHAR(64),
    detail VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_admin_audit_created ON admin_audit_log (created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_log (target_user_id);
