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

    @Getter
    @Builder
    public static class CategoryStat {
        private final String category;
        private final long count;
        private final double averageIntensity;
    }
}
