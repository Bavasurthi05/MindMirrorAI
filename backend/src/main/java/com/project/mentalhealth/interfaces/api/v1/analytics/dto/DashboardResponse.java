package com.project.mentalhealth.interfaces.api.v1.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Everything the dashboard renders, in one call.
 *
 * <p>Nullable fields are deliberate: a null means "we do not have the data to say", and the UI
 * must render an empty state rather than substitute a plausible-looking number. Nothing in this
 * response is a default or a placeholder.
 */
@Getter
@Builder
public class DashboardResponse {

    /** 0-100, or null when there is not enough data to compute one. */
    private final Integer wellnessScore;

    /** Change in percentage points vs. the previous 7 days; null when either window is empty. */
    private final Integer wellnessDelta;

    private final Instant wellnessUpdatedAt;

    /** Band and one-line reading of the composite score (see MindsetResponse). */
    private final String mindsetBand;
    private final String mindsetInsight;

    /** Human-readable one-liner built from the user's actual activity. */
    private final String headline;

    private final Summary summary;
    private final List<MoodDay> moodWeek;
    private final List<RecentAnalysis> recentAnalyses;
    private final List<Labelled> triggerSummary;
    private final List<Recommendation> recommendations;
    private final List<Progress> progressItems;
    private final DataCompleteness dataCompleteness;
    private final CheckIn checkIn;

    /** Auto-detected triggers awaiting the user's confirm/dismiss verdict. */
    private final int pendingTriggerCount;

    @Getter
    @Builder
    public static class Summary {
        /** "Low" / "Medium" / "High", or null when no triggers have been recorded. */
        private final String stress;
        /** "Low" / "Moderate" / "High", or null when no mood data exists. */
        private final String energy;
        /** "Building" / "Steady" / "Consistent", or null when nothing has been journaled. */
        private final String reflection;
    }

    @Getter
    @Builder
    public static class MoodDay {
        private final String date;
        private final String label;
        /** Null on days with no entry — the UI shows a gap, not a zero. */
        private final Integer score;
    }

    @Getter
    @Builder
    public static class RecentAnalysis {
        private final Long id;
        private final String date;
        private final String headline;
        private final String detail;
        private final String prediction;
        private final String sentiment;
    }

    @Getter
    @Builder
    public static class Labelled {
        private final String label;
        private final String value;
    }

    @Getter
    @Builder
    public static class Recommendation {
        private final Long id;
        private final String title;
        private final String detail;
    }

    @Getter
    @Builder
    public static class Progress {
        private final String label;
        /** Null when the underlying signal is missing; the UI shows "Not enough data". */
        private final Integer progress;
    }

    /** Drives the onboarding prompts that tell the user what to do next. */
    @Getter
    @Builder
    public static class DataCompleteness {
        private final boolean hasJournal;
        private final boolean hasMood;
        private final boolean hasAssessment;
        private final boolean hasTriggers;
        private final boolean hasAnalysis;
        private final long daysOfData;
        private final long pendingAnalyses;
        private final boolean onboardingCompleted;
        /** Null when never assessed; drives the re-assessment nudge. */
        private final Long daysSinceAssessment;
    }

    /** State of today's check-in, so the card shows at most once per local day. */
    @Getter
    @Builder
    public static class CheckIn {
        private final boolean checkedInToday;
        private final int streak;
        private final Integer todaysScore;
    }
}
