package com.project.mentalhealth.domain.model;

import com.project.mentalhealth.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Per-user settings that shape collection and presentation.
 *
 * <p>Timezone matters more than it looks: every daily bucket (streaks, heatmaps, the mood week,
 * "one check-in per day") was previously computed in UTC, which puts the day boundary in the
 * middle of the evening for a large part of the world.
 */
@Getter
@Setter
@Entity
@Table(name = "user_preferences")
public class UserPreferences extends BaseEntity {

    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String DEFAULT_REMINDER_TIME = "20:00";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** IANA zone id, e.g. {@code Australia/Sydney}. */
    @Column(nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled;

    /** Local 24h "HH:mm" the browser schedules the daily nudge for. */
    @Column(name = "reminder_time", nullable = false, length = 5)
    private String reminderTime = DEFAULT_REMINDER_TIME;

    /** Comma-separated focus areas chosen during onboarding. */
    @Column(name = "focus_areas", length = 512)
    private String focusAreas;

    /**
     * Opt-in for using this user's text to train models. Default off, revocable, and
     * deliberately independent of analysis: declining must not degrade the product.
     */
    @Column(name = "training_consent", nullable = false)
    private boolean trainingConsent;

    @Column(name = "training_consent_at")
    private Instant trainingConsentAt;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    /** Resume point for the onboarding wizard. */
    @Column(name = "onboarding_step", nullable = false)
    private int onboardingStep;
}
