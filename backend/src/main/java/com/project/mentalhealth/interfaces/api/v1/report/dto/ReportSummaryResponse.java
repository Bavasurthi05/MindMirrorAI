package com.project.mentalhealth.interfaces.api.v1.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportSummaryResponse {
    private final int overallWellness;
    private final String stressTrend;
    private final int recoveryScore;
    private final long journalCount;
    private final double averageMood;
    private final String latestSeverity;
    private final String summaryText;
    private final List<TriggerStrength> topTriggers;
    private final List<String> recommendations;
    private final List<String> predictionSummary;

    @Getter
    @Builder
    public static class TriggerStrength {
        private final String title;
        private final String strength;
    }
}
