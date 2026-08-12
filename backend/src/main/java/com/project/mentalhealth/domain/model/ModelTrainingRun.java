package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Audit record for a retraining run.
 *
 * <p>Deploying a model changes what every user is told about their own mind, so who
 * triggered it, what it learned from, how it scored and who promoted it must stay
 * answerable after the fact.
 */
@Getter
@Setter
@Entity
@Table(name = "model_training_runs")
public class ModelTrainingRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", length = 64)
    private String jobId;

    @Column(length = 64)
    private String version;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "corpus_total", nullable = false)
    private int corpusTotal;

    @Column(name = "corpus_user_examples", nullable = false)
    private int corpusUserExamples;

    private Double accuracy;

    @Column(name = "f1_macro")
    private Double f1Macro;

    @Column(name = "gate_passed", nullable = false)
    private boolean gatePassed;

    @Lob
    @Column(name = "gate_detail", columnDefinition = "TEXT")
    private String gateDetail;

    @Column(nullable = false)
    private boolean promoted;

    @Column(name = "promoted_at")
    private Instant promotedAt;

    @Column(name = "promoted_by")
    private String promotedBy;

    /** True when an admin knowingly deployed a version that failed the gate. */
    @Column(nullable = false)
    private boolean forced;

    @Column(length = 1000)
    private String message;
}
