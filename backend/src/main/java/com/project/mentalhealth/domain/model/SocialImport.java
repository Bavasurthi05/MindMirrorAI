package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One uploaded data export.
 *
 * <p>The skip counters exist so the user can be told plainly why fewer posts were imported
 * than they expected — "12 were reposts of other people's posts" rather than a silent gap.
 */
@Getter
@Setter
@Entity
@Table(name = "social_imports")
public class SocialImport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SocialProvider provider;

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SocialImportStatus status = SocialImportStatus.PROCESSING;

    /** Posts the parser found in the file, before any filtering. */
    @Column(name = "posts_found", nullable = false)
    private int postsFound;

    /** New posts saved for analysis. */
    @Column(name = "posts_imported", nullable = false)
    private int postsImported;

    @Column(name = "skipped_duplicates", nullable = false)
    private int skippedDuplicates;

    /** Reposts of someone else's content, which are not the user's own words. */
    @Column(name = "skipped_reposts", nullable = false)
    private int skippedReposts;

    /** Posts with no text, or too little to analyze. */
    @Column(name = "skipped_short", nullable = false)
    private int skippedShort;

    @Column(name = "skipped_over_limit", nullable = false)
    private int skippedOverLimit;

    @Column(name = "earliest_post_at")
    private Instant earliestPostAt;

    @Column(name = "latest_post_at")
    private Instant latestPostAt;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
