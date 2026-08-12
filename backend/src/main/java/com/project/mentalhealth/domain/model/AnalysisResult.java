package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A persisted ML analysis of a piece of user text.
 *
 * <p>Rows start as {@link AnalysisStatus#PENDING} the moment the source text is saved, so the
 * write path never waits on the ML service. A background worker fills in the result and flips
 * the status to {@code OK} or {@code FAILED}.
 */
@Getter
@Setter
@Entity
@Table(name = "analysis_results")
public class AnalysisResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 24)
    private AnalysisSourceType sourceType;

    /** Id of the originating record (journal entry, social import, …); null for ad-hoc text. */
    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Lob
    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(length = 24)
    private String sentiment;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "dominant_emotion", length = 48)
    private String dominantEmotion;

    @Column(name = "emotion_scores", columnDefinition = "JSON")
    private String emotionScores;

    @Column(length = 32)
    private String prediction;

    @Column(name = "prediction_confidence")
    private Double predictionConfidence;

    @Column(name = "prediction_probabilities", columnDefinition = "JSON")
    private String predictionProbabilities;

    @Column(columnDefinition = "JSON")
    private String reasons;

    /** Token-level sentiment contributions shown in the "why this prediction" view. */
    @Column(columnDefinition = "JSON")
    private String explanation;

    @Column(name = "detected_triggers", columnDefinition = "JSON")
    private String detectedTriggers;

    @Column(name = "model_backend", length = 48)
    private String modelBackend;

    @Column(name = "model_version", length = 64)
    private String modelVersion;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;
}
