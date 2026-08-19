package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A record of one privileged change to an account.
 *
 * <p>Emails are snapshotted rather than joined: the point of an audit row is to stay
 * readable even if the account is later renamed or deleted.
 */
@Getter
@Setter
@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog extends BaseEntity {

    public static final String ACTION_ROLE_CHANGED = "ROLE_CHANGED";
    public static final String ACTION_ACCESS_CHANGED = "ACCESS_CHANGED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String action;

    /** Who performed the change. */
    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_email")
    private String targetEmail;

    @Column(name = "previous_value", length = 64)
    private String previousValue;

    @Column(name = "new_value", length = 64)
    private String newValue;

    @Column(length = 512)
    private String detail;
}
