package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A user's own normal range, recomputed nightly.
 *
 * <p>Used to express insights as deviation ("1.4σ below your usual") rather than as an
 * absolute label, which is both more useful and more defensible for a non-clinical tool.
 * {@link #established} stays false until there is enough history to mean anything.
 */
@Getter
@Setter
@Entity
@Table(name = "user_baselines")
public class UserBaseline extends BaseEntity {

    /** Below this many days of data, a "baseline" would just be noise. */
    public static final int MIN_DAYS_FOR_BASELINE = 14;
    public static final int MIN_SAMPLES_FOR_BASELINE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    @Column(name = "days_covered", nullable = false)
    private int daysCovered;

    @Column(name = "mean_sentiment")
    private Double meanSentiment;

    @Column(name = "stddev_sentiment")
    private Double stdDevSentiment;

    @Column(name = "mean_mood")
    private Double meanMood;

    @Column(name = "stddev_mood")
    private Double stdDevMood;

    @Column(name = "mean_trigger_intensity")
    private Double meanTriggerIntensity;

    @Column(name = "journaling_days_per_week")
    private Double journalingDaysPerWeek;

    /** Per-state prediction rates as JSON, e.g. {"stress":0.4,"normal":0.6}. */
    @Column(name = "state_rates", columnDefinition = "JSON")
    private String stateRates;

    /** False while still gathering history — the UI says "building your baseline". */
    @Column(nullable = false)
    private boolean established;

    @Column(name = "computed_at")
    private Instant computedAt;
}
