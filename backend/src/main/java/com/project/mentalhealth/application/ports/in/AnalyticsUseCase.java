package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse;

public interface AnalyticsUseCase {
    AnalyticsOverviewResponse overview(String userEmail);
    MlAnalysisPort.WeeklyInsights weeklyInsights(String userEmail);
}
