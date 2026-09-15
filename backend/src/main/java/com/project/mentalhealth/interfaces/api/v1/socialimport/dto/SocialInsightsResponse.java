package com.project.mentalhealth.interfaces.api.v1.socialimport.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * What a user's imported posts say.
 *
 * <p>The timeline is built from when posts were published, not when they were analyzed: a
 * whole archive is analyzed at once, so analysis time would put years of posts on one day.
 */
@Getter
@Builder
public class SocialInsightsResponse {

    private final int totalPosts;
    private final int analyzedPosts;
    private final int pendingPosts;
    /** Analyzed posts whose export carried no date, so they are absent from the timeline. */
    private final int undatedPosts;

    private final Map<String, Long> postsByProvider;
    private final Map<String, Long> predictionDistribution;
    /** Mean sentiment from -1 to 1; null before anything has been analyzed. */
    private final Double averageSentiment;
    private final List<EmotionCount> topEmotions;
    private final List<MonthPoint> timeline;
    private final List<PostInsight> recentPosts;

    @Getter
    @Builder
    public static class EmotionCount {
        private final String emotion;
        private final long count;
    }

    @Getter
    @Builder
    public static class MonthPoint {
        /** ISO year-month, e.g. "2026-03". */
        private final String month;
        private final String label;
        private final int posts;
        private final double averageSentiment;
        /** Share of the month's posts that read as stress, anxiety or low mood (0–1). */
        private final double concernShare;
    }

    @Getter
    @Builder
    public static class PostInsight {
        private final Long id;
        private final Long analysisId;
        private final String provider;
        private final Instant postedAt;
        private final String excerpt;
        private final String sentiment;
        private final Double sentimentScore;
        private final String prediction;
        private final Double predictionConfidence;
        private final String dominantEmotion;
    }
}
