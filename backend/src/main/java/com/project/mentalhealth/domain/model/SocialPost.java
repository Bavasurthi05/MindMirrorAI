package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A single post the user wrote, taken from their own data export. */
@Getter
@Setter
@Entity
@Table(name = "social_posts")
public class SocialPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_id", nullable = false)
    private SocialImport socialImport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SocialProvider provider;

    @Column(name = "external_post_id", nullable = false, length = 128)
    private String externalPostId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** When it was published; null when the export carried no date. */
    @Column(name = "posted_at")
    private Instant postedAt;

    /** Set once analysis has been attempted. */
    @Column(name = "analysis_result_id")
    private Long analysisResultId;
}
