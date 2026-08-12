package com.project.mentalhealth.interfaces.api.v1.analytics;

import com.project.mentalhealth.application.ports.in.AnalyticsUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.application.service.DashboardService;
import com.project.mentalhealth.application.service.MindsetScoringService;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.DashboardResponse;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.MindsetResponse;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/analytics")
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;
    private final DashboardService dashboardService;
    private final MindsetScoringService mindsetScoringService;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase,
                               DashboardService dashboardService,
                               MindsetScoringService mindsetScoringService) {
        this.analyticsUseCase = analyticsUseCase;
        this.dashboardService = dashboardService;
        this.mindsetScoringService = mindsetScoringService;
    }

    /** The composite, personalized wellbeing score — the one number the app agrees on. */
    @GetMapping("/mindset")
    public ApiResponse<MindsetResponse> mindset(Authentication authentication) {
        return ApiResponse.success(mindsetScoringService.mindset(authentication.getName()));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success(dashboardService.dashboard(authentication.getName()));
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverviewResponse> overview(Authentication authentication) {
        return ApiResponse.success(analyticsUseCase.overview(authentication.getName()));
    }

    @GetMapping("/weekly-insights")
    public ApiResponse<MlAnalysisPort.WeeklyInsights> weeklyInsights(Authentication authentication) {
        return ApiResponse.success(analyticsUseCase.weeklyInsights(authentication.getName()));
    }
}
