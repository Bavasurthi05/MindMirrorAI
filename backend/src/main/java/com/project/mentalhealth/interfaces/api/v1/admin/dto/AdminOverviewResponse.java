package com.project.mentalhealth.interfaces.api.v1.admin.dto;

import lombok.Builder;
import lombok.Getter;

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
}
