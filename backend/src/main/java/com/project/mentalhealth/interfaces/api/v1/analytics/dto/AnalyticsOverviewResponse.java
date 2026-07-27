package com.project.mentalhealth.interfaces.api.v1.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsOverviewResponse {

    private final int overallWellness;
    private final List<Metric> radar;
    private final List<HeatCell> heatmap;
    private final List<EmotionPoint> emotionTimeline;
    private final List<Metric> weeklyTrend;
    private final List<SeriesPoint> moodSeries;
    private final List<Metric> emotionDistribution;
    private final List<Metric> triggerDistribution;

    @Getter
    @Builder
    public static class Metric {
        private final String label;
        private final double value;
    }

    @Getter
    @Builder
    public static class SeriesPoint {
        private final String date;
        private final int score;
    }

    @Getter
    @Builder
    public static class HeatCell {
        private final String date;
        private final Integer score; // null when no data for that day
    }

    @Getter
    @Builder
    public static class EmotionPoint {
        private final String date;
        private final String label;
        private final int score;
    }
}
