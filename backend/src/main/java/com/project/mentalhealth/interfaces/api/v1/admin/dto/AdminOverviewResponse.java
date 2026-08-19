package com.project.mentalhealth.interfaces.api.v1.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminOverviewResponse {

    private final long totalUsers;
    private final long verifiedUsers;
    private final long totalJournalEntries;
    private final long totalMoodEntries;
    private final long totalAssessments;
    private final long totalTriggers;
    private final long totalRecoveryActions;

    /** Real sign-ups per month for the trailing 6 months, oldest first. */
    private final List<GrowthPoint> userGrowth;

    /** Trigger categories across all users, most common first. */
    private final List<CategoryCount> triggerDistribution;

    @Getter
    @Builder
    public static class GrowthPoint {
        /** e.g. "Mar 2026". */
        private final String label;
        private final long newUsers;
        private final long cumulativeUsers;
    }

    @Getter
    @Builder
    public static class CategoryCount {
        private final String category;
        private final long count;
    }
}
