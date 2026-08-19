package com.project.mentalhealth.interfaces.api.v1.trigger.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TriggerAnalyticsResponse {

    private final long totalCount;
    private final double averageIntensity;
    private final List<CategoryStat> categories;

    /**
     * Average intensity by weekday × time-of-day, in the user's own timezone.
     *
     * <p>Cells with no triggers carry a null intensity so the UI can render "no data" rather
     * than a misleading cool colour.
     */
    private final List<HeatCell> heatmap;

    /** Average intensity per weekday (Mon–Sun), null where nothing was logged. */
    private final List<WeekdayStat> weekdayIntensity;

    @Getter
    @Builder
    public static class CategoryStat {
        private final String category;
        private final long count;
        private final double averageIntensity;
    }

    @Getter
    @Builder
    public static class HeatCell {
        /** MONDAY … SUNDAY. */
        private final String day;
        /** Morning · Midday · Evening · Night. */
        private final String slot;
        private final Double averageIntensity;
        private final long count;
    }

    @Getter
    @Builder
    public static class WeekdayStat {
        private final String day;
        private final Double averageIntensity;
        private final long count;
    }
}
