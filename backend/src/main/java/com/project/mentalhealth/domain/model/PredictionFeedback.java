package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A user's verdict on a model prediction.
 *
 * <p>A {@code DISAGREE} carrying a {@link #correctedLabel} is a directly supervised training
 * example; an {@code AGREE} confirms the predicted label. Both are worth far more than any
 * signal the model produces about itself.
 */
@Getter
@Setter
@Entity
@Table(name = "prediction_feedback")
public class PredictionFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_result_id", nullable = false)
    private AnalysisResult analysisResult;

    /** Snapshotted so the label survives a later re-analysis of the same text. */
    @Column(name = "predicted_label", length = 32)
    private String predictedLabel;

    /** What the user says it should have been; null when they agreed. */
    @Column(name = "corrected_label", length = 32)
    private String correctedLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeedbackAgreement agreement;

    @Column(length = 1000)
    private String comment;
}
