package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "social_accounts")
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "external_account_id", nullable = false, length = 160)
    private String externalAccountId;

    @Column(name = "display_name", length = 160)
    private String displayName;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(nullable = false, length = 32)
    private String status = "CONNECTED";

    @Column(name = "connected_at")
    private java.time.Instant connectedAt;

    @Column(name = "last_synced_at")
    private java.time.Instant lastSyncedAt;

    @Column(name = "oauth_scope", length = 512)
    private String oauthScope;

    @Column(name = "token_reference", length = 1024)
    private String tokenReference;
}
